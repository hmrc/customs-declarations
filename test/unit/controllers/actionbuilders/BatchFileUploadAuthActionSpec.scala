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

import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, errorBadRequest}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames.{XBadgeIdentifierHeaderName, XEoriIdentifierHeaderName}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{BatchFileUploadAuthAction, HeaderValidator}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AnalyticsValuesAndConversationIdRequest
import uk.gov.hmrc.customs.declaration.model.{BatchFileUploadCsp, GoogleAnalyticsValues}
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._
import util.{AuthConnectorNrsDisabledStubbing, AuthConnectorStubbing, RequestHeaders}

class BatchFileUploadAuthActionSpec extends UnitSpec with MockitoSugar {

  private val errorResponseBadgeIdentifierHeaderMissing =
    errorBadRequest(s"$XBadgeIdentifierHeaderName header is missing or invalid")
  private val errorResponseEoriIdentifierHeaderMissing =
    errorBadRequest(s"$XEoriIdentifierHeaderName header is missing or invalid")

  private lazy val validatedHeadersRequestWithValidBadgeIdEoriPair =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair()).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdEoriPair =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair(eoriString = "", badgeIdString = "")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithValidBadgeIdAndEmptyEori =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair(eoriString = "")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithValidBadgeIdAndEoriTooLong =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair(eoriString = "INVALID_EORI_TOO_LONG")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidEoriInvalidChars =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair(eoriString = "(*&*(^&*&%")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdTooLong =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair(badgeIdString = "INVALID_BADGE_IDENTIFIER_TOO_LONG")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdLowerCase =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair(badgeIdString = "lowercase")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdTooShort =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair(badgeIdString = "SHORT")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdInvalidChars =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, testFakeRequestWithBadgeIdEoriPair(badgeIdString = "(*&*(^&*&%")).toValidatedHeadersRequest(TestExtractedHeaders)

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockGoogleAnalyticsConnector: GoogleAnalyticsConnector = mock[GoogleAnalyticsConnector]
    val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
  }

  trait NrsEnabled extends AuthConnectorStubbing with SetUp {
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockGoogleAnalyticsConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
    val batchFileUploadAuthAction = new BatchFileUploadAuthAction(customsAuthService, headerValidator, mockLogger, mockGoogleAnalyticsConnector, mockDeclarationConfigService)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
  }

  trait NrsDisabled extends AuthConnectorNrsDisabledStubbing with SetUp {
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockGoogleAnalyticsConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
    val batchFileUploadAuthAction = new BatchFileUploadAuthAction(customsAuthService, headerValidator, mockLogger, mockGoogleAnalyticsConnector, mockDeclarationConfigService)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigDisabled)
  }

  "AuthAction Builder " can {

    "as CSP when NRS is enabled" should {
      "authorise as CSP when authorised by auth API and both badge identifier and eori exists" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdEoriPair))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeIdEoriPair.toCspAuthorisedRequest(BatchFileUploadCsp(badgeIdentifier, declarantEori, Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but both badge identifier and eori are invalid" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdEoriPair))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdEoriPair)
      }

      "Return 401 response when authorised by auth API but badge identifier does not exist" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(TestValidatedHeadersRequestNoBadge)
      }

      "Return 401 response when authorised by auth API but badge identifier does not exist and eori does" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(TestValidatedHeadersRequestWithEoriAndNoBadgeId))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(TestValidatedHeadersRequestWithEoriAndNoBadgeId)
      }

      "Return 401 response when authorised by auth API but badge identifier is too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdTooLong)
      }

      "Return 401 response when authorised by auth API but badge identifier is too short" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdTooShort)
      }

      "Return 401 response when authorised by auth API but badge identifier contains invalid chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdInvalidChars)
      }

      "Return 401 response when authorised by auth API but badge identifier contains all lowercase chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdLowerCase)
      }

      "Return 401 response when authorised by auth API where badge identifier exists and eori does not" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(TestValidatedHeadersRequestWithBadgeIdAndNoEori))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriIdentifierHeaderMissing.message)(TestValidatedHeadersRequestWithBadgeIdAndNoEori)
      }

      "Return 401 response when authorised by auth API with empty eori" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdAndEmptyEori))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriIdentifierHeaderMissing.message)(validatedHeadersRequestWithValidBadgeIdAndEmptyEori)
      }

      "Return 401 response when authorised by auth API with eori too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdAndEoriTooLong))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriIdentifierHeaderMissing.message)(validatedHeadersRequestWithValidBadgeIdAndEoriTooLong)
      }

      "Return 401 response when authorised by auth API with eori containing invalid characters" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidEoriInvalidChars))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidEoriInvalidChars)
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsEnabled {
        authoriseCspError()

        private val actual = await(batchFileUploadAuthAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

    }

    "as CSP when NRS is disabled" should {
      "authorise as CSP when authorised by auth API and badge identifier exists" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdEoriPair))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeIdEoriPair.toCspAuthorisedRequest(BatchFileUploadCsp(badgeIdentifier, declarantEori, None)))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier does not exist" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(TestValidatedHeadersRequestNoBadge)
      }

      "Return 401 response when authorised by auth API but badge identifier does not exist and eori does" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(TestValidatedHeadersRequestWithEoriAndNoBadgeId))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(TestValidatedHeadersRequestWithEoriAndNoBadgeId)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but is too long" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdTooLong)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but is too short" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdTooShort)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdInvalidChars)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidBadgeIdLowerCase)
      }

      "Return 401 response when authorised by auth API where badge identifier exists and eori does not" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(TestValidatedHeadersRequestWithBadgeIdAndNoEori))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriIdentifierHeaderMissing.message)(TestValidatedHeadersRequestWithBadgeIdAndNoEori)
      }

      "Return 401 response when authorised by auth API empty eori" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdAndEmptyEori))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriIdentifierHeaderMissing.message)(validatedHeadersRequestWithValidBadgeIdAndEmptyEori)
      }

      "Return 401 response when authorised by auth API with eori too long" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithValidBadgeIdAndEoriTooLong))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriIdentifierHeaderMissing.message)(validatedHeadersRequestWithValidBadgeIdAndEoriTooLong)
      }

      "Return 401 response when authorised by auth API with eori containing invalid characters" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(batchFileUploadAuthAction.refine(validatedHeadersRequestWithInvalidEoriInvalidChars))

        actual shouldBe Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriIdentifierHeaderMissing.message)(validatedHeadersRequestWithInvalidEoriInvalidChars)
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsDisabled {
        authoriseCspError()

        private val actual = await(batchFileUploadAuthAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }
  }
}
