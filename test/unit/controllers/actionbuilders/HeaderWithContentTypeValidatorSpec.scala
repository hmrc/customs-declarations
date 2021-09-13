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

import org.scalatest.{Matchers}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames._
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.HeaderWithContentTypeValidator
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.RequestHeaders.{ValidHeadersV2, _}
import util.{ApiSubscriptionFieldsTestData, TestData}

class HeaderWithContentTypeValidatorSpec extends AnyWordSpecLike with TableDrivenPropertyChecks with MockitoSugar with Matchers {

  private val extractedHeadersWithBadgeIdentifierV1 = ExtractedHeadersImpl(ApiSubscriptionFieldsTestData.clientId)

  trait SetUp {
    val loggerMock: DeclarationsLogger = mock[DeclarationsLogger]
    val validator = new HeaderWithContentTypeValidator(loggerMock)

    def validate(avr: ApiVersionRequest[_]): Either[ErrorResponse, ExtractedHeaders] = {
      validator.validateHeaders(avr)
    }
  }

  "HeaderWithContentTypeValidator" can {
    "in happy path, validation" should {
      "be successful for content type XML with no space header" in new SetUp {
        validate(apiVersionRequest(ValidHeadersV1 + (CONTENT_TYPE -> "application/xml;charset=utf-8"))) shouldBe Right(extractedHeadersWithBadgeIdentifierV1)
      }
    }
    "in unhappy path, validation" should {
      "fail when request is missing content type header" in new SetUp {
        validate(apiVersionRequest(ValidHeadersV2 - CONTENT_TYPE)) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "fail when request is missing X-Client-ID header" in new SetUp {
        validate(apiVersionRequest(ValidHeadersV2 - XClientIdHeaderName)) shouldBe Left(ErrorInternalServerError)
      }
      "fail when request has invalid content type header (for JSON)" in new SetUp {
        validate(apiVersionRequest(ValidHeadersV2 + CONTENT_TYPE_HEADER_INVALID)) shouldBe Left(ErrorContentTypeHeaderInvalid)
      }
      "fail when request has invalid X-Client-ID header" in new SetUp {
        validate(apiVersionRequest(ValidHeadersV2 + X_CLIENT_ID_HEADER_INVALID)) shouldBe Left(ErrorInternalServerError)
      }
    }

    "in validating the mandatory eori header" should {
      "not allow an empty header" in new SetUp {
        private val value = validator.eoriMustBeValidAndPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "")))

        value shouldBe Left(errorBadRequest(s"$X_SUBMITTER_IDENTIFIER_NAME header is missing or invalid"))
      }

      "not allow only spaces in the header" in new SetUp {
        private val value = validator.eoriMustBeValidAndPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "       ")))

        value shouldBe Left(errorBadRequest(s"$X_SUBMITTER_IDENTIFIER_NAME header is missing or invalid"))
      }

      "not allow headers longer than 17 characters" in new SetUp {
        private val value = validator.eoriMustBeValidAndPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "012345678901234567")))

        value shouldBe Left(errorBadRequest(s"$X_SUBMITTER_IDENTIFIER_NAME header is missing or invalid"))
      }

      "allow headers with leading spaces" in new SetUp {
        private val value = validator.eoriMustBeValidAndPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "  0123456789")))

        value shouldBe Right(Some(Eori("  0123456789")))
      }

      "allow headers with trailing spaces" in new SetUp {
        private val value = validator.eoriMustBeValidAndPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "0123456789    ")))

        value shouldBe Right(Some(Eori("0123456789    ")))
      }

      "allow headers with embedded spaces" in new SetUp {
        private val value = validator.eoriMustBeValidAndPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "01234  56789")))

        value shouldBe Right(Some(Eori("01234  56789")))
      }

      "allow special characters" in new SetUp {
        private val value = validator.eoriMustBeValidAndPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "!£$%^&*()-_=+/<>@")))

        value shouldBe Right(Some(Eori("!£$%^&*()-_=+/<>@")))
      }
    }
    
    "in validating the optional eori header" should {
      "allow a missing header" in new SetUp {
        private val value = validator.eoriMustBeValidIfPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 - X_SUBMITTER_IDENTIFIER_NAME))

        value shouldBe Right(None)
      }

      "allow an empty header" in new SetUp {
        private val value = validator.eoriMustBeValidIfPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "")))

        value shouldBe Right(None)
      }

      "allow only spaces in the header but treat as empty" in new SetUp {
        private val value = validator.eoriMustBeValidIfPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "       ")))

        value shouldBe Right(None)
      }

      "not allow headers longer than 17 characters" in new SetUp {
        private val value = validator.eoriMustBeValidIfPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "012345678901234567")))

        value shouldBe Left(errorBadRequest(s"$X_SUBMITTER_IDENTIFIER_NAME header is invalid"))
      }

      "allow headers with leading spaces" in new SetUp {
        private val value = validator.eoriMustBeValidIfPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "  0123456789")))

        value shouldBe Right(Some(Eori("  0123456789")))
      }

      "allow headers with trailing spaces" in new SetUp {
        private val value = validator.eoriMustBeValidIfPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "0123456789    ")))

        value shouldBe Right(Some(Eori("0123456789    ")))
      }

      "allow headers with embedded spaces" in new SetUp {
        private val value = validator.eoriMustBeValidIfPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "01234  56789")))

        value shouldBe Right(Some(Eori("01234  56789")))
      }

      "allow special characters" in new SetUp {
        private val value = validator.eoriMustBeValidIfPresent(X_SUBMITTER_IDENTIFIER_NAME)(apiVersionRequest(ValidHeadersV2 + (X_SUBMITTER_IDENTIFIER_NAME -> "!£$%^&*()-_=+/<>@")))

        value shouldBe Right(Some(Eori("!£$%^&*()-_=+/<>@")))
      }
    }
  }

  private def apiVersionRequest(requestMap: Map[String, String]): ApiVersionRequest[_] =
    ApiVersionRequest(TestData.conversationId, EventStart, VersionOne, FakeRequest().withHeaders(requestMap.toSeq: _*))
}
