/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.logging

import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames._
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames
import uk.gov.hmrc.customs.declaration.logging.LoggingHelper
import uk.gov.hmrc.customs.declaration.model.AuthorisedAs.AuthorisedAs
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ConversationIdRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declaration.model.{AuthorisedAs, ClientId, VersionOne}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._

class LoggingHelperSpec extends UnitSpec with MockitoSugar {

  private def expectedMessage(message: String, maybeAuthorised: Option[AuthorisedAs] = None) = s"[conversationId=${conversationId.toString}]" +
    "[clientId=some-client-id]" +
    s"[requestedApiVersion=1.0]${maybeAuthorised.fold("")(authAs => s"[authorisedAs=$authAs]")} $message"
  private val requestMock = mock[Request[_]]
  private val conversationIdRequest =
    ConversationIdRequest(
      conversationId,
      FakeRequest().withHeaders(
        CONTENT_TYPE -> "A",
        ACCEPT -> "B",
        CustomHeaderNames.XConversationIdHeaderName -> "C",
        CustomHeaderNames.XClientIdHeaderName -> "D",
        "IGNORE" -> "IGNORE"
      )
    )
  private val validatedHeadersRequest = ValidatedHeadersRequest(conversationId, None, VersionOne, ClientId("some-client-id"), requestMock)

  "LoggingHelper" should {


    "testFormatInfo" in {
      LoggingHelper.formatInfo("Info message", validatedHeadersRequest) shouldBe expectedMessage("Info message")
    }

    "testFormatInfo with authorisation" in {
      val x = LoggingHelper.formatInfo("Info message", validatedHeadersRequest.toCspAuthorisedRequest)
      LoggingHelper.formatInfo("Info message", validatedHeadersRequest.toCspAuthorisedRequest) shouldBe expectedMessage("Info message", Some(AuthorisedAs.Csp))
    }

    "testFormatError" in {
      LoggingHelper.formatError("Error message", validatedHeadersRequest) shouldBe expectedMessage("Error message")
    }

    "testFormatWarn" in {
      LoggingHelper.formatWarn("Warn message", validatedHeadersRequest) shouldBe expectedMessage("Warn message")
    }

    "testFormatDebug" in {
      LoggingHelper.formatDebug("Debug message", validatedHeadersRequest) shouldBe expectedMessage("Debug message")
    }

    "testFormatDebugFull" in {
      LoggingHelper.formatDebugFull("Debug message.", conversationIdRequest) shouldBe s"[conversationId=$conversationIdValue] Debug message. headers=Map(Accept -> B, X-Client-ID -> D, Content-Type -> A, X-Conversation-ID -> C)"
    }
  }
}
