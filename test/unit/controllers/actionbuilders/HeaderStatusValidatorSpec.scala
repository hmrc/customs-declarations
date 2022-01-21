/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.mvc.Http.Status.BAD_REQUEST
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.HeaderStatusValidator
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.VersionOne
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ApiVersionRequest, ExtractedHeaders}
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.RequestHeaders.{ValidHeadersV2, _}
import util.{TestData}

class HeaderStatusValidatorSpec extends AnyWordSpecLike with TableDrivenPropertyChecks with MockitoSugar with Matchers{

  private val ErrorInvalidBadgeIdentifierHeader: ErrorResponse = ErrorResponse(BAD_REQUEST, BadRequestCode, s"X-Badge-Identifier header is missing or invalid")

  trait SetUp {
    val loggerMock: DeclarationsLogger = mock[DeclarationsLogger]
    val validator = new HeaderStatusValidator(loggerMock)

    def validate(avr: ApiVersionRequest[_]): Either[ErrorResponse, ExtractedHeaders] = {
      validator.validateHeaders(avr)
    }
  }

  "HeaderValidator" can {
    "in unhappy path, validation" should {
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
