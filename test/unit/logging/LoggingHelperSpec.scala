/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames._
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames
import uk.gov.hmrc.customs.declaration.logging.LoggingHelper
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ConversationIdRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declaration.model.{ClientId, Csp, VersionOne}
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.TestData._

class LoggingHelperSpec extends AnyWordSpecLike with MockitoSugar with Matchers {

  private def expectedMessage(message: String, maybeAuthorised: String = "") = s"[conversationId=${conversationId.toString}]" +
    "[clientId=some-client-id]" +
    s"[requestedApiVersion=1.0]$maybeAuthorised $message"
  private val requestMock = mock[Request[?]]
  private val conversationIdRequest =
    ConversationIdRequest(
      conversationId,
      EventStart,
      FakeRequest().withHeaders(
        CONTENT_TYPE -> "A",
        ACCEPT -> "B",
        CustomHeaderNames.XConversationIdHeaderName -> "C",
        CustomHeaderNames.XClientIdHeaderName -> "D",
        "IGNORE" -> "IGNORE"
      )
    )
  private val validatedHeadersRequest = ValidatedHeadersRequest(conversationId, EventStart, VersionOne, ClientId("some-client-id"), requestMock)

  "LoggingHelper" should {


    "testFormatInfo" in {
      LoggingHelper.formatInfo("Info message", validatedHeadersRequest) shouldBe expectedMessage("Info message")
    }

    "testFormatInfo with authorisation" in {
      LoggingHelper.formatInfo("Info message", validatedHeadersRequest.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), Some(nrsRetrievalValues)))) shouldBe expectedMessage("Info message", "[authorisedAs=Csp(Some(ZZ123456789000), Some(BADGEID123))]")
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
      LoggingHelper.formatDebugFull("Debug message.", conversationIdRequest) shouldBe s"[conversationId=$conversationIdValue] Debug message. headers=TreeMap(Accept -> B, X-Client-ID -> D, Content-Type -> A, X-Conversation-ID -> C)"
    }
  }

}
