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
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.HeaderValidator
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ConversationIdRequest, ExtractedHeadersImpl}
import uk.gov.hmrc.customs.declaration.model.{VersionOne, VersionTwo}
import uk.gov.hmrc.play.test.UnitSpec
import util.RequestHeaders._
import util.TestData.badgeIdentifier
import util.{ApiSubscriptionFieldsTestData, RequestHeaders, TestData}

class HeaderValidatorSpec extends UnitSpec with TableDrivenPropertyChecks with MockitoSugar {

  private val extractedHeadersWithBadgeIdentifierV1 = ExtractedHeadersImpl(Some(badgeIdentifier), VersionOne, ApiSubscriptionFieldsTestData.clientId)
  private val extractedHeadersWithBadgeIdentifierV2 = extractedHeadersWithBadgeIdentifierV1.copy(requestedApiVersion = VersionTwo)
  private val extractedHeadersWithoutBadgeIdentifier = extractedHeadersWithBadgeIdentifierV2.copy(maybeBadgeIdentifier = None)
  private val errorResponseBadgeIdentifierHeader = errorBadRequest(s"${RequestHeaders.X_BADGE_IDENTIFIER_NAME} header is missing or invalid")

  trait SetUp {
    val loggerMock: DeclarationsLogger = mock[DeclarationsLogger]
    val validator = new HeaderValidator(loggerMock)
  }

  val headersTable =
    Table(
      ("description", "Headers", "Expected response"),
      ("Valid Headers for V1", ValidHeadersV1, Right(extractedHeadersWithBadgeIdentifierV1)),
      ("Valid Headers for V2", ValidHeadersV2, Right(extractedHeadersWithBadgeIdentifierV2)),
      ("Valid content type XML with no space header", ValidHeadersV2 + (CONTENT_TYPE -> "application/xml;charset=utf-8"), Right(extractedHeadersWithBadgeIdentifierV2)),
      ("Missing accept header", ValidHeadersV2 - ACCEPT, Left(ErrorAcceptHeaderInvalid)),
      ("Missing content type header", ValidHeadersV2 - CONTENT_TYPE, Left(ErrorContentTypeHeaderInvalid)),
      ("Missing X-Client-ID header", ValidHeadersV2 - XClientIdHeaderName, Left(ErrorInternalServerError)),
      ("Missing X-Badge-Identifier header", ValidHeadersV2 - XBadgeIdentifierHeaderName, Right(extractedHeadersWithoutBadgeIdentifier)),
      ("Invalid accept header", ValidHeadersV2 + ACCEPT_HEADER_INVALID, Left(ErrorAcceptHeaderInvalid)),
      ("Invalid content type header JSON header", ValidHeadersV2 + CONTENT_TYPE_HEADER_INVALID, Left(ErrorContentTypeHeaderInvalid)),
      ("Invalid X-Client-ID header", ValidHeadersV2 + X_CLIENT_ID_HEADER_INVALID, Left(ErrorInternalServerError)),
      ("Invalid X-Badge-Identifier header - too short", ValidHeadersV2 + X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_SHORT, Left(errorResponseBadgeIdentifierHeader)),
      ("Invalid X-Badge-Identifier header - too long", ValidHeadersV2 + X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG, Left(errorResponseBadgeIdentifierHeader)),
      ("Invalid X-Badge-Identifier header - lowercase", ValidHeadersV2 + X_BADGE_IDENTIFIER_HEADER_INVALID_LOWERCASE, Left(errorResponseBadgeIdentifierHeader)),
      ("Invalid X-Badge-Identifier header - invalid characters", ValidHeadersV2 + X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS, Left(errorResponseBadgeIdentifierHeader))
    )

  "HeaderValidatorAction" should {
    forAll(headersTable) { (description, headers, response) =>
      s"$description" in new SetUp {
        private val conversationIdRequest: ConversationIdRequest[_] = ConversationIdRequest(TestData.conversationId, FakeRequest().withHeaders(headers.toSeq: _*))

        validator.validateHeaders(conversationIdRequest) shouldBe response
      }
    }
  }
}
