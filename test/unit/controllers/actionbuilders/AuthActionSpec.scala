/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.http.Status.UNAUTHORIZED
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode, errorBadRequest}
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames.{XBadgeIdentifierHeaderName, XSubmitterIdentifierHeaderName}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{AuthAction, AuthActionCustomHeader, AuthActionSubmitterHeader, HeaderWithContentTypeValidator}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.Csp
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}
import uk.gov.hmrc.play.test.UnitSpec
import util.CustomsDeclarationsMetricsTestData._
import util.RequestHeaders.X_SUBMITTER_IDENTIFIER_NAME
import util.TestData._
import util.{AuthConnectorNrsDisabledStubbing, AuthConnectorStubbing, RequestHeaders}

class AuthActionSpec extends UnitSpec with MockitoSugar {

  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")
  private val errorResponseBadgeIdentifierHeaderMissing =
    errorBadRequest(s"${CustomHeaderNames.XBadgeIdentifierHeaderName} header is missing or invalid")
  private val errorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")
  private val errorResponseMissingIdentifiers =
    errorBadRequest(s"Both $XSubmitterIdentifierHeaderName and $XBadgeIdentifierHeaderName are missing")
  private val errorResponseEoriIdentifierHeaderInvalid =
    errorBadRequest("X-Submitter-Identifier header is invalid")

  
  private lazy val validatedHeadersRequestWithValidBadgeId =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithBadgeId()).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithValidBadgeIdEoriPair =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithBadgeIdEoriPair(eoriHeaderName = X_SUBMITTER_IDENTIFIER_NAME)).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithValidEoriNoBadgeId =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithEoriNoBadgeId(eoriHeaderName = X_SUBMITTER_IDENTIFIER_NAME)).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidEoriNoBadgeId =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithInvalidEoriNoBadgeId(eoriHeaderName = X_SUBMITTER_IDENTIFIER_NAME)).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdNoEori =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithInvalidBadgeIdNoEori()).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdTooLong =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithBadgeId(badgeIdString = "INVALID_BADGE_IDENTIFIER_TO_LONG")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdLowerCase =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithBadgeId(badgeIdString = "lowercase")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdTooShort =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithBadgeId(badgeIdString = "SHORT")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdInvalidChars =
    ConversationIdRequest(conversationId, EventStart, testFakeRequestWithBadgeId(badgeIdString = "(*&*(^&*&%")).toValidatedHeadersRequest(TestExtractedHeaders)

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    protected implicit val ec = Helpers.stubControllerComponents().executionContext
  }

  trait NrsEnabled extends AuthConnectorStubbing with SetUp {
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderWithContentTypeValidator(mockLogger)
    val authAction: AuthAction = new AuthAction(customsAuthService, headerValidator, mockLogger, mockDeclarationConfigService)
    val authActionSubmitterHeader: AuthAction = new AuthActionSubmitterHeader(customsAuthService, headerValidator, mockLogger, mockDeclarationConfigService)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
  }

  trait NrsDisabled extends AuthConnectorNrsDisabledStubbing with SetUp {
    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderWithContentTypeValidator(mockLogger)
    val authAction: AuthAction = new AuthAction(customsAuthService, headerValidator, mockLogger, mockDeclarationConfigService)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigDisabled)
  }

  "AuthAction Builder " can {
    "as CSP with no eori when NRS is enabled" should {
      "authorise as CSP when authorised by auth API and badge identifier exists" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithValidBadgeId))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeId.toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier), Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier does not exist" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but is too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but is too short" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsEnabled {
        authoriseCspError()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }
    
    "as CSP with eori when NRS is enabled" should {
      "authorise as CSP when authorised by auth API and badge identifier and eori exist" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithValidBadgeIdEoriPair))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeIdEoriPair.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled
      }

      "authorise as CSP when authorised by auth API and badge identifier exists and eori does not" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithValidBadgeId))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeId.toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier), Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled
      }

      "authorise as CSP when authorised by auth API and badge identifier does not exist and eori does" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithValidEoriNoBadgeId))
        actual shouldBe Right(validatedHeadersRequestWithValidEoriNoBadgeId.toCspAuthorisedRequest(Csp(Some(declarantEori), None, Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled
      }

      "authorise as CSP when authorised by auth API and badge identifier does not exist and eori does but is too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidEoriNoBadgeId))
        actual shouldBe Left(errorResponseEoriIdentifierHeaderInvalid.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "authorise as CSP when authorised by auth API and badge identifier is too long and eori exists" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdNoEori))
        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but neither badge identifier nor eori exist" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseMissingIdentifiers.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but is too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but is too short" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }

    "as CSP when NRS is disabled" should {
      "authorise as CSP when authorised by auth API and badge identifier exists" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithValidBadgeId))
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeId.toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier), None)))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier does not exists" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }


      "Return 401 response when authorised by auth API but badge identifier exists but is too long" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but is too short" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
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
      }

      "Return 401 response when not authorised as NonCsp" in new NrsEnabled {
        unauthoriseCsp()
        unauthoriseNonCspOnly()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseUnauthorisedGeneral.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
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
      }

      "Return 401 response when not authorised as NonCsp" in new NrsDisabled {
        unauthoriseCsp()
        unauthoriseNonCspOnly()

        private val actual = await(authAction.refine(TestValidatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseUnauthorisedGeneral.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
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
