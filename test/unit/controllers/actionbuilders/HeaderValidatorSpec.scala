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

package unit.controllers.actionbuilders

import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.HeaderNames._
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.HeaderValidator
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AnalyticsValuesAndConversationIdRequest, ExtractedHeaders, ExtractedHeadersImpl}
import uk.gov.hmrc.customs.declaration.model.{GoogleAnalyticsValues, VersionOne, VersionThree, VersionTwo}
import uk.gov.hmrc.play.test.UnitSpec
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.RequestHeaders.{ValidHeadersV2, _}
import util.{ApiSubscriptionFieldsTestData, TestData}

class HeaderValidatorSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  private val extractedHeadersWithBadgeIdentifierV1 = ExtractedHeadersImpl(VersionOne, ApiSubscriptionFieldsTestData.clientId)
  private val extractedHeadersWithBadgeIdentifierV2 = extractedHeadersWithBadgeIdentifierV1.copy(requestedApiVersion = VersionTwo)
  private val extractedHeadersWithBadgeIdentifierV3 = extractedHeadersWithBadgeIdentifierV1.copy(requestedApiVersion = VersionThree)

  trait SetUp {
    val loggerMock: DeclarationsLogger = mock[DeclarationsLogger]
    val validator = new HeaderValidator(loggerMock)

    def validate(c: AnalyticsValuesAndConversationIdRequest[_]): Either[ErrorResponse, ExtractedHeaders] = {
      validator.validateHeaders(c)
    }
  }

  "HeaderValidator" can {
    "in happy path, validation" should {
      "be successful for a valid request with accept header for V1" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV1)) shouldBe Right(extractedHeadersWithBadgeIdentifierV1)
      }
      "be successful for a valid request with accept header for V2" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV2)) shouldBe Right(extractedHeadersWithBadgeIdentifierV2)
      }
      "be successful for a valid request with accept header for V3" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV3)) shouldBe Right(extractedHeadersWithBadgeIdentifierV3)
      }
      "be successful for content type XML with no space header" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV2 + (CONTENT_TYPE -> "application/xml;charset=utf-8"))) shouldBe Right(extractedHeadersWithBadgeIdentifierV2)
      }
    }
    "in unhappy path, validation" should {
      "fail when request is missing accept header" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV2 - ACCEPT)) shouldBe Left(ErrorAcceptHeaderInvalid)
      }
      "fail when request is missing content type header" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV2 - CONTENT_TYPE)) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "fail when request is missing X-Client-ID header" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV2 - XClientIdHeaderName)) shouldBe Left(ErrorInternalServerError)
      }
      "fail when request has invalid accept header" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV2 + ACCEPT_HEADER_INVALID)) shouldBe Left(ErrorAcceptHeaderInvalid)
      }
      "fail when request has invalid content type header (for JSON)" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV2 + CONTENT_TYPE_HEADER_INVALID)) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "fail when request has invalid X-Client-ID header" in new SetUp {
        validate(analyticsValuesAndConversationIdRequest(ValidHeadersV2 + X_CLIENT_ID_HEADER_INVALID)) shouldBe Left(ErrorInternalServerError)
      }
    }
  }

  private def analyticsValuesAndConversationIdRequest(requestMap: Map[String, String]): AnalyticsValuesAndConversationIdRequest[_] =
    AnalyticsValuesAndConversationIdRequest(TestData.conversationId, GoogleAnalyticsValues.Submit, EventStart, FakeRequest().withHeaders(requestMap.toSeq: _*))
}
