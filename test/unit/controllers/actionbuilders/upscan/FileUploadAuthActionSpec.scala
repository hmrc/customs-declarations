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

package unit.controllers.actionbuilders.upscan

import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, errorBadRequest}
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames.{XBadgeIdentifierHeaderName, XEoriIdentifierHeaderName}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{AuthActionEoriHeader, HeaderWithContentTypeValidator}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.model.{Csp, VersionOne}
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.TestData._
import util.{AuthConnectorNrsDisabledStubbing, AuthConnectorStubbing, RequestHeaders, UnitSpec}

import scala.concurrent.ExecutionContext

class FileUploadAuthActionSpec extends WordSpec with MockitoSugar with Matchers {

  private val errorResponseBadgeIdentifierHeaderMissing =
    errorBadRequest(s"$XBadgeIdentifierHeaderName header is missing or invalid")
  private val errorResponseEoriIdentifierHeaderMissing =
    errorBadRequest(s"$XEoriIdentifierHeaderName header is missing or invalid")

  private lazy val validatedHeadersRequestWithValidBadgeIdEoriPair =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair()).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdEoriPair =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(eoriString = "", badgeIdString = "")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithValidBadgeIdAndEmptyEori =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(eoriString = "")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithValidBadgeIdAndEoriTooLong =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(eoriString = "INVALID_EORI_TOO_LONG")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidEoriInvalidChars =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(eoriString = "     ")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdTooLong =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(badgeIdString = "INVALID_BADGE_IDENTIFIER_TOO_LONG")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdLowerCase =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(badgeIdString = "lowercase")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdTooShort =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(badgeIdString = "SHORT")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdInvalidChars =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(badgeIdString = "(*&*(^&*&%")).toValidatedHeadersRequest(TestExtractedHeaders)

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  }

  trait NrsEnabled extends AuthConnectorStubbing with SetUp {
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderWithContentTypeValidator(mockLogger)
    val fileUploadAuthAction = new AuthActionEoriHeader(customsAuthService, headerValidator, mockLogger, mockDeclarationConfigService)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
  }

  trait NrsDisabled extends AuthConnectorNrsDisabledStubbing with SetUp {
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderWithContentTypeValidator(mockLogger)
    val fileUploadAuthAction = new AuthActionEoriHeader(customsAuthService, headerValidator, mockLogger, mockDeclarationConfigService)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigDisabled)
  }

  "AuthAction Builder " can {

    "as CSP when NRS is enabled" should {
      "authorise as CSP when authorised by auth API and both badge identifier and eori exists" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdEoriPair))

        actual shouldBe Right(validatedHeadersRequestWithValidBadgeIdEoriPair.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), None)))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but both badge identifier and eori are invalid" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdEoriPair))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier does not exist" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        val validatedHeadersRequestNoBadge = TestConversationIdRequest.toApiVersionRequest(VersionOne).toValidatedHeadersRequest(TestExtractedHeaders)

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier does not exist and eori does" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(TestValidatedHeadersRequestWithEoriAndNoBadgeId))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier is too long" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier is too short" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier contains invalid chars" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier contains all lowercase chars" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API where badge identifier exists and eori does not" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(TestValidatedHeadersRequestWithBadgeIdAndNoEori))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API with empty eori" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdAndEmptyEori))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API with eori too long" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdAndEoriTooLong))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API with eori containing invalid characters" in new NrsEnabled {
        authoriseCspButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidEoriInvalidChars))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsEnabled {
        authoriseCspErrorButDontFetchRetrievals()

        private val actual = await(fileUploadAuthAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

    }

    "as CSP when NRS is disabled" should {
      "authorise as CSP when authorised by auth API and badge identifier exists" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdEoriPair))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeIdEoriPair.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), None)))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier does not exist" in new NrsDisabled {
        authoriseCsp()

        val validatedHeadersRequestNoBadge = TestConversationIdRequest.toApiVersionRequest(VersionOne).toValidatedHeadersRequest(TestExtractedHeaders)
        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier does not exist and eori does" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(TestValidatedHeadersRequestWithEoriAndNoBadgeId))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but is too long" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but is too short" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API where badge identifier exists and eori does not" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(TestValidatedHeadersRequestWithBadgeIdAndNoEori))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API empty eori" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdAndEmptyEori))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API with eori too long" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdAndEoriTooLong))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API with eori containing invalid characters" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(fileUploadAuthAction.refine(validatedHeadersRequestWithInvalidEoriInvalidChars))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsDisabled {
        authoriseCspError()

        private val actual = await(fileUploadAuthAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }
  }
}
