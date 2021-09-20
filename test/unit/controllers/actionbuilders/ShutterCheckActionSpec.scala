/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.controllers.actionbuilders


import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.SERVICE_UNAVAILABLE
import play.api.mvc.Result
import play.api.test.Helpers.{ACCEPT, await, defaultAwaitTimeout}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorAcceptHeaderInvalid
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.ShutterCheckAction
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.DeclarationsShutterConfig
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.RequestHeaders.{ACCEPT_HEADER_INVALID, ValidHeadersV1, X_CONVERSATION_ID_NAME}
import util.TestData._

import scala.concurrent.ExecutionContext

class ShutterCheckActionSpec extends AnyWordSpecLike with MockitoSugar with Matchers{

  trait SetUp {
    protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    val mockConfigService = mock[DeclarationsConfigService]
    private val mockLogger = mock[DeclarationsLogger]
    val errorResponseVersionShuttered: Result = ErrorResponse(SERVICE_UNAVAILABLE, "SERVER_ERROR", "Service Unavailable").XmlResult

    val allVersionsShuttered = DeclarationsShutterConfig(Some(true), Some(true), Some(true))
    val versionOneShuttered = DeclarationsShutterConfig(Some(true), Some(false), Some(false))
    val versionTwoShuttered = DeclarationsShutterConfig(Some(false), Some(true), Some(false))
    val versionThreeShuttered = DeclarationsShutterConfig(Some(false), Some(false), Some(true))
    val allVersionsShutteringUnspecified = DeclarationsShutterConfig(None, None, None)

    when(mockConfigService.declarationsShutterConfig).thenReturn(allVersionsUnshuttered)

    val action = new ShutterCheckAction(mockLogger, mockConfigService)
  }
  
  "in happy path, validation" should {
    "be successful for a valid request with accept header for V1" in new SetUp {
      await(action.refine(TestConversationIdRequestWithV1Headers)) shouldBe Right(TestApiVersionRequestV1)
    }

    "be successful for a valid request with accept header for V2" in new SetUp {
      await(action.refine(TestConversationIdRequestWithV2Headers)) shouldBe Right(TestApiVersionRequestV2)
    }

    "be successful for a valid request with accept header for V3" in new SetUp {
      await(action.refine(TestConversationIdRequestWithV3Headers)) shouldBe Right(TestApiVersionRequestV3)
    }
  }

  "in unhappy path, validation" should {
    "fail for a valid request with missing accept header" in new SetUp {
      val requestWithoutAcceptHeader = FakeRequest().withXmlBody(TestXmlPayload).withHeaders((ValidHeadersV1 - ACCEPT).toSeq: _*)
      
      val result = await(action.refine(ConversationIdRequest(conversationId, EventStart, requestWithoutAcceptHeader)))
      result shouldBe Left(ErrorAcceptHeaderInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationIdValue))
    }

    "fail for a valid request with invalid accept header" in new SetUp {
      val requestWithInvalidAcceptHeader = FakeRequest().withXmlBody(TestXmlPayload).withHeaders((ValidHeadersV1 + ACCEPT_HEADER_INVALID).toSeq: _*)

      val result = await(action.refine(ConversationIdRequest(conversationId, EventStart, requestWithInvalidAcceptHeader)))
      result shouldBe Left(ErrorAcceptHeaderInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationIdValue))
    }
  }
  
  "when shuttered set" should {
    "return 503 error for a valid request with v1 accept header and v1 is shuttered" in new SetUp {
      when(mockConfigService.declarationsShutterConfig).thenReturn(versionOneShuttered)
      val result = await(action.refine(TestConversationIdRequestWithV1Headers))

      result shouldBe Left(errorResponseVersionShuttered)
    }

    "return 503 error for a valid request with v2 accept header and v2 is shuttered" in new SetUp {
      when(mockConfigService.declarationsShutterConfig).thenReturn(versionTwoShuttered)
      val result = await(action.refine(TestConversationIdRequestWithV2Headers))

      result shouldBe Left(errorResponseVersionShuttered)
    }

    "return 503 error for a valid request with v3 accept header and v3 is shuttered" in new SetUp {
      when(mockConfigService.declarationsShutterConfig).thenReturn(versionThreeShuttered)
      val result = await(action.refine(TestConversationIdRequestWithV3Headers))

      result shouldBe Left(errorResponseVersionShuttered)
    }

    "return 503 error for a valid request with v2 accept header and all versions are shuttered" in new SetUp {
      when(mockConfigService.declarationsShutterConfig).thenReturn(allVersionsShuttered)
      val result = await(action.refine(TestConversationIdRequestWithV2Headers))

      result shouldBe Left(errorResponseVersionShuttered)
    }

    "be successful when a valid request with v1 accept header and no shuttering is unspecified" in new SetUp {
      when(mockConfigService.declarationsShutterConfig).thenReturn(allVersionsShutteringUnspecified)
      val result = await(action.refine(TestConversationIdRequestWithV1Headers))

      result shouldBe Right(TestApiVersionRequestV1)
    }
  }
  
}
