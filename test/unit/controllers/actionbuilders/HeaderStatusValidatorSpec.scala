/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.mvc.Http.Status.BAD_REQUEST
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.HeaderStatusValidator
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.VersionOne
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ApiVersionRequest, ExtractedHeaders, ExtractedStatusHeadersImpl}
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.RequestHeaders.{ValidHeadersV2, _}
import util.TestData.badgeIdentifier
import util.{ApiSubscriptionFieldsTestData, TestData, UnitSpec}

class HeaderStatusValidatorSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  //TODO move these tests based on apiversion elsewhere
  private val extractedHeadersWithBadgeIdentifierV2 = ExtractedStatusHeadersImpl(badgeIdentifier, ApiSubscriptionFieldsTestData.clientId)
//  private val extractedHeadersWithBadgeIdentifierV3 = extractedHeadersWithBadgeIdentifierV2.copy(requestedApiVersion = VersionThree)
  private val ErrorInvalidBadgeIdentifierHeader: ErrorResponse = ErrorResponse(BAD_REQUEST, BadRequestCode, s"X-Badge-Identifier header is missing or invalid")

  trait SetUp {
    val loggerMock: DeclarationsLogger = mock[DeclarationsLogger]
    val validator = new HeaderStatusValidator(loggerMock)

    def validate(avr: ApiVersionRequest[_]): Either[ErrorResponse, ExtractedHeaders] = {
      validator.validateHeaders(avr)
    }
  }

  "HeaderValidator" can {
    "in happy path, validation" should {
      "be successful for a valid request with accept header for V2" in new SetUp {
        validate(apiVersionRequest(ValidHeadersV2)) shouldBe Right(extractedHeadersWithBadgeIdentifierV2)
      }
      "be successful for a valid request with accept header for V3" in new SetUp {
//        validate(apiVersionRequest(ValidHeadersV3)) shouldBe Right(extractedHeadersWithBadgeIdentifierV3)
      }
    }
    "in unhappy path, validation" should {

      //TODO should be moved to ShutterCheckActionSpec
//      "fail when request is for V1" in new SetUp {
//        validate(apiVersionRequest(ValidHeadersV1)) shouldBe Left(ErrorAcceptHeaderInvalid)
//      }
      "fail when request has invalid X-Badge-Identifier header" in new SetUp {
        validate(apiVersionRequest(ValidHeadersV2 + X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_SHORT)) shouldBe Left(ErrorInvalidBadgeIdentifierHeader)
      }
      "fail when request has missing X-Badge-Identifier header" in new SetUp {
        validate(apiVersionRequest(ValidHeadersV2 - X_BADGE_IDENTIFIER_NAME)) shouldBe Left(ErrorInvalidBadgeIdentifierHeader)
      }
    }
  }

  private def apiVersionRequest(requestMap: Map[String, String]): ApiVersionRequest[_] =
    ApiVersionRequest(TestData.conversationId, EventStart, VersionOne, FakeRequest().withHeaders(requestMap.toSeq: _*))
}
