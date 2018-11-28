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
import play.api.http.Status
import play.api.http.Status.UNAUTHORIZED
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode, errorBadRequest}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{AuthAction, HeaderValidator}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AnalyticsValuesAndConversationIdRequest
import uk.gov.hmrc.customs.declaration.model.{Csp, GoogleAnalyticsValues}
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}
import uk.gov.hmrc.play.test.UnitSpec
import util.CustomsDeclarationsMetricsTestData._
import util.TestData._
import util.{AuthConnectorNrsDisabledStubbing, AuthConnectorStubbing, CustomsDeclarationsMetricsTestData, RequestHeaders}

class AuthActionSpec extends UnitSpec with MockitoSugar {

  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")
  private val errorResponseBadgeIdentifierHeaderMissing =
    errorBadRequest(s"${CustomHeaderNames.XBadgeIdentifierHeaderName} header is missing or invalid")
  private lazy val errorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")

  private lazy val validatedHeadersRequestWithValidBadgeId =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, testFakeRequestWithBadgeId()).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInValidBadgeIdTooLong =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, testFakeRequestWithBadgeId(badgeIdString = "INVALID_BADGE_IDENTIFIER_TO_LONG")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInValidBadgeIdLowerCase =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, testFakeRequestWithBadgeId(badgeIdString = "lowercase")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInValidBadgeIdTooShort =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, testFakeRequestWithBadgeId(badgeIdString = "SHORT")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInValidBadgeIdInvalidChars =
    AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, testFakeRequestWithBadgeId(badgeIdString = "(*&*(^&*&%")).toValidatedHeadersRequest(TestExtractedHeaders)

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockGoogleAnalyticsConnector: GoogleAnalyticsConnector = mock[GoogleAnalyticsConnector]
    val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
  }

  trait NrsEnabled extends AuthConnectorStubbing with SetUp {
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockGoogleAnalyticsConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
    val authAction: AuthAction = new AuthAction(customsAuthService, headerValidator, mockLogger, mockGoogleAnalyticsConnector, mockDeclarationConfigService)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
  }

  trait NrsDisabled extends AuthConnectorNrsDisabledStubbing with SetUp {
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockGoogleAnalyticsConnector, mockLogger)
    protected val headerValidator = new HeaderValidator(mockLogger)
    val authAction: AuthAction = new AuthAction(customsAuthService, headerValidator, mockLogger, mockGoogleAnalyticsConnector, mockDeclarationConfigService)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigDisabled)
  }

  "AuthAction Builder " can {
    "as CSP when NRS is enabled" should {
      "authorise as CSP when authorised by auth API and badge identifier exists" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithValidBadgeId))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeId.toCspAuthorisedRequest(Csp(badgeIdentifier, Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier does not exists" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(TestValidatedHeadersRequestNoBadge)
      }


      "Return 401 response when authorised by auth API but badge identifier exists but is too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInValidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInValidBadgeIdTooLong)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but is too short" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInValidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInValidBadgeIdTooShort)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInValidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInValidBadgeIdInvalidChars)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInValidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInValidBadgeIdLowerCase)
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsEnabled {
        authoriseCspError()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }

    "as CSP when NRS is disabled" should {
      "authorise as CSP when authorised by auth API and badge identifier exists" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithValidBadgeId))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeId.toCspAuthorisedRequest(Csp(badgeIdentifier, None)))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier does not exists" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(TestValidatedHeadersRequestNoBadge)
      }


      "Return 401 response when authorised by auth API but badge identifier exists but is too long" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInValidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInValidBadgeIdTooLong)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but is too short" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInValidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInValidBadgeIdTooShort)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInValidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInValidBadgeIdInvalidChars)
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInValidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
        verify(mockGoogleAnalyticsConnector).failure(errorResponseBadgeIdentifierHeaderMissing.message)(validatedHeadersRequestWithInValidBadgeIdLowerCase)
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsDisabled {
        authoriseCspError()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }

    "As a Non CSP when NRS is enabled" should {
      "Authorise as Non CSP when authorised by auth API" in new NrsEnabled {
        authoriseNonCsp(Some(declarantEori))

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Right(TestValidatedHeadersRequestNoBadge.toNonCspAuthorisedRequest(declarantEori, Some(nrsRetrievalValues)))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when authorised by auth API but Eori not exists" in new NrsEnabled {
        authoriseNonCsp(maybeEori = None)

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseEoriNotFoundInCustomsEnrolment.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriNotFoundInCustomsEnrolment.message)(TestValidatedHeadersRequestNoBadge)
      }

      "Return 401 response when not authorised as NonCsp" in new NrsEnabled {
        unauthoriseCsp()
        unauthoriseNonCspOnly()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseUnauthorisedGeneral.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
        verify(mockGoogleAnalyticsConnector).failure(errorResponseUnauthorisedGeneral.message)(TestValidatedHeadersRequestNoBadge)
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsEnabled {
        unauthoriseCsp()
        authoriseNonCspOnlyError()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }
    }

    "As a Non CSP with NRS disabled" should {
      "Authorise as Non CSP when authorised by auth API" in new NrsDisabled {
        authoriseNonCsp(Some(declarantEori))

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Right(TestValidatedHeadersRequestNoBadge.toNonCspAuthorisedRequest(declarantEori, None))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when authorised by auth API but Eori not exists" in new NrsDisabled {
        authoriseNonCsp(maybeEori = None)

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseEoriNotFoundInCustomsEnrolment.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
        verify(mockGoogleAnalyticsConnector).failure(errorResponseEoriNotFoundInCustomsEnrolment.message)(TestValidatedHeadersRequestNoBadge)
      }

      "Return 401 response when not authorised as NonCsp" in new NrsDisabled {
        unauthoriseCsp()
        unauthoriseNonCspOnly()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseUnauthorisedGeneral.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
        verify(mockGoogleAnalyticsConnector).failure(errorResponseUnauthorisedGeneral.message)(TestValidatedHeadersRequestNoBadge)
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsDisabled {
        unauthoriseCsp()
        authoriseNonCspOnlyError()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }
    }
  }
}
