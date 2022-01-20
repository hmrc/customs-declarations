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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.http.Status.UNAUTHORIZED
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode, errorBadRequest}
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames.{XBadgeIdentifierHeaderName, XSubmitterIdentifierHeaderName}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{AuthAction, AuthActionSubmitterHeader, HeaderWithContentTypeValidator}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ApiVersionRequest, HasConversationId}
import uk.gov.hmrc.customs.declaration.model.{Csp, VersionOne}
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}
import util.CustomsDeclarationsMetricsTestData._
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.RequestHeaders.{X_CONVERSATION_ID_NAME, X_SUBMITTER_IDENTIFIER_NAME}
import util.TestData._
import util.{AuthConnectorNrsDisabledStubbing, AuthConnectorStubbing, UnitSpec}

import scala.Console.in
import scala.concurrent.ExecutionContext

class AuthActionSpec extends AnyWordSpecLike with MockitoSugar with Matchers{

  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")
  private val errorResponseBadgeIdentifierHeaderMissing =
    errorBadRequest(s"${CustomHeaderNames.XBadgeIdentifierHeaderName} header is missing or invalid")
  private val errorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")
  private val errorResponseMissingIdentifiers =
    errorBadRequest(s"Both $XSubmitterIdentifierHeaderName and $XBadgeIdentifierHeaderName headers are missing")
  private val errorResponseEoriIdentifierHeaderInvalid =
    errorBadRequest("X-Submitter-Identifier header is invalid")

  
  private lazy val validatedHeadersRequestWithValidBadgeId =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeId()).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithValidBadgeIdEoriPair =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeIdEoriPair(eoriHeaderName = X_SUBMITTER_IDENTIFIER_NAME)).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithValidEoriNoBadgeId =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithEoriNoBadgeId(eoriHeaderName = X_SUBMITTER_IDENTIFIER_NAME)).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidEoriNoBadgeId =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithInvalidEoriNoBadgeId(eoriHeaderName = X_SUBMITTER_IDENTIFIER_NAME)).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdNoEori =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithInvalidBadgeIdNoEori()).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdTooLong =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeId(badgeIdString = "INVALID_BADGE_IDENTIFIER_TO_LONG")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdLowerCase =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeId(badgeIdString = "lowercase")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdTooShort =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeId(badgeIdString = "SHORT")).toValidatedHeadersRequest(TestExtractedHeaders)
  private lazy val validatedHeadersRequestWithInvalidBadgeIdInvalidChars =
    ApiVersionRequest(conversationId, EventStart, VersionOne, testFakeRequestWithBadgeId(badgeIdString = "(*&*(^&*&%")).toValidatedHeadersRequest(TestExtractedHeaders)

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
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

        PassByNameVerifier(mockLogger, "info")
          .withByNameParam[String]("X-Badge-Identifier header passed validation: BADGEID123")
          .withParamMatcher[HasConversationId](any[HasConversationId])
          .verify()
      }

      "Return 400 response when authorised by auth API but badge identifier does not exist" in new NrsEnabled {
        authoriseCsp()

        val validatedHeadersRequestNoBadge = TestConversationIdRequest.toApiVersionRequest(VersionOne).toValidatedHeadersRequest(TestExtractedHeaders)
        
        private val actual = await(authAction.refine(validatedHeadersRequestNoBadge))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but is too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but is too short" in new NrsEnabled {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort))

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authAction.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authAction.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsEnabled {
        authoriseCspError()

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }
    
    "as CSP with eori when NRS is enabled" should {
      "authorise as CSP when authorised by auth API and badge identifier and eori exist" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithValidBadgeIdEoriPair)).futureValue
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeIdEoriPair.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled

        PassByNameVerifier(mockLogger, "info")
          .withByNameParam[String]("X-Submitter-Identifier header passed validation: ZZ123456789000\nX-Badge-Identifier header passed validation: BADGEID123")
          .withParamMatcher[HasConversationId](any[HasConversationId])
          .verify()
      }

      "authorise as CSP when authorised by auth API and badge identifier exists and eori does not" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithValidBadgeId)).futureValue
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeId.toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier), Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled

        PassByNameVerifier(mockLogger, "info")
          .withByNameParam[String]("X-Submitter-Identifier header not present or is empty\nX-Badge-Identifier header passed validation: BADGEID123")
          .withParamMatcher[HasConversationId](any[HasConversationId])
          .verify()
      }

      "authorise as CSP when authorised by auth API and badge identifier does not exist and eori does" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithValidEoriNoBadgeId)).futureValue
        actual shouldBe Right(validatedHeadersRequestWithValidEoriNoBadgeId.toCspAuthorisedRequest(Csp(Some(declarantEori), None, Some(nrsRetrievalValues))))
        verifyNonCspAuthorisationNotCalled

        PassByNameVerifier(mockLogger, "info")
          .withByNameParam[String]("X-Submitter-Identifier header passed validation: ZZ123456789000\nX-Badge-Identifier header empty and allowed")
          .withParamMatcher[HasConversationId](any[HasConversationId])
          .verify()
      }

      "Return 400 response when authorised by auth API and badge identifier does not exist and eori does but is too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidEoriNoBadgeId)).futureValue
        actual shouldBe Left(errorResponseEoriIdentifierHeaderInvalid.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API and badge identifier is too long and eori exists" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdNoEori)).futureValue
        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but neither badge identifier nor eori exist" in new NrsEnabled {
        authoriseCsp()
        val validatedHeadersRequestNoBadge = TestConversationIdRequest.toApiVersionRequest(VersionOne).toValidatedHeadersRequest(TestExtractedHeaders)

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(errorResponseMissingIdentifiers.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but is too long" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but is too short" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 400 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsEnabled {
        authoriseCsp()

        private val actual = (authActionSubmitterHeader.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }

    "as CSP when NRS is disabled" should {
      "authorise as CSP when authorised by auth API and badge identifier exists" in new NrsDisabled {
        authoriseCsp()

        private val actual = (authAction.refine(validatedHeadersRequestWithValidBadgeId)).futureValue
        actual shouldBe Right(validatedHeadersRequestWithValidBadgeId.toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier), None)))
        verifyNonCspAuthorisationNotCalled

        PassByNameVerifier(mockLogger, "info")
          .withByNameParam[String]("X-Badge-Identifier header passed validation: BADGEID123")
          .withParamMatcher[HasConversationId](any[HasConversationId])
          .verify()
      }

      "Return 401 response when authorised by auth API but badge identifier does not exists" in new NrsDisabled {
        authoriseCsp()
        val validatedHeadersRequestNoBadge = TestConversationIdRequest.toApiVersionRequest(VersionOne).toValidatedHeadersRequest(TestExtractedHeaders)

        private val actual = (authAction.refine(validatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }


      "Return 401 response when authorised by auth API but badge identifier exists but is too long" in new NrsDisabled {
        authoriseCsp()

        private val actual = (authAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooLong)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but is too short" in new NrsDisabled {
        authoriseCsp()

        private val actual = (authAction.refine(validatedHeadersRequestWithInvalidBadgeIdTooShort)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains invalid chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = (authAction.refine(validatedHeadersRequestWithInvalidBadgeIdInvalidChars)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 401 response when authorised by auth API but badge identifier exists but contains all lowercase chars" in new NrsDisabled {
        authoriseCsp()

        private val actual = (authAction.refine(validatedHeadersRequestWithInvalidBadgeIdLowerCase)).futureValue

        actual shouldBe Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsDisabled {
        authoriseCspError()

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyNonCspAuthorisationNotCalled
      }
    }

    "As a Non CSP when NRS is enabled" should {
      "Authorise as Non CSP when authorised by auth API" in new NrsEnabled {
        authoriseNonCsp(Some(declarantEori))

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Right(TestValidatedHeadersRequestNoBadge.toNonCspAuthorisedRequest(declarantEori, Some(nrsRetrievalValues)))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when authorised by auth API but Eori not exists" in new NrsEnabled {
        authoriseNonCsp(maybeEori = None)

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(errorResponseEoriNotFoundInCustomsEnrolment.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when not authorised as NonCsp" in new NrsEnabled {
        unauthoriseCsp()
        unauthoriseNonCspOnly()

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(errorResponseUnauthorisedGeneral.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsEnabled {
        unauthoriseCsp()
        authoriseNonCspOnlyError()

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }
    }

    "As a Non CSP with NRS disabled" should {
      "Authorise as Non CSP when authorised by auth API" in new NrsDisabled {
        authoriseNonCsp(Some(declarantEori))

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Right(TestValidatedHeadersRequestNoBadge.toNonCspAuthorisedRequest(declarantEori, None))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when authorised by auth API but Eori not exists" in new NrsDisabled {
        authoriseNonCsp(maybeEori = None)

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(errorResponseEoriNotFoundInCustomsEnrolment.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }

      "Return 401 response when not authorised as NonCsp" in new NrsDisabled {
        unauthoriseCsp()
        unauthoriseNonCspOnly()

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(errorResponseUnauthorisedGeneral.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }

      "Return 500 response if errors occur in CSP auth API call" in new NrsDisabled {
        unauthoriseCsp()
        authoriseNonCspOnlyError()

        private val actual = (authAction.refine(TestValidatedHeadersRequestNoBadge)).futureValue

        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(X_CONVERSATION_ID_NAME -> conversationId.toString))
        verifyCspAuthorisationCalled(1)
      }
    }
  }
}
