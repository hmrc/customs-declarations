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

package unit.controllers

import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, Retrievals}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.BadRequestCode
import uk.gov.hmrc.customs.declaration.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomsDeclarationsController
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.services.{CustomsConfigService, CustomsDeclarationsBusinessService, RequestedVersionService, UuidService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.RequestHeaders
import util.RequestHeaders._
import util.TestData._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class CustomsDeclarationsControllerSpec extends UnitSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  private val mockDeclarationsLogger = mock[DeclarationsLogger]
  private val mockCustomsConfigService = mock[CustomsConfigService]
  private val mockAuthConnector = mock[MicroserviceAuthConnector]
  private val mockRequestedVersionService = mock[RequestedVersionService]
  private val mockCustomsDeclarationsBusinessService = mock[CustomsDeclarationsBusinessService]
  private val mockUuidService = mock[UuidService]

  private val controller = new CustomsDeclarationsController(mockDeclarationsLogger,
    mockCustomsConfigService,
    mockAuthConnector,
    mockRequestedVersionService,
    mockCustomsDeclarationsBusinessService,
    mockUuidService
  )

  private val apiScope = "scope-in-api-definition"
  private val customsEnrolmentName = "HMRC-CUS-ORG"
  private val eoriIdentifier = "EORINumber"
  private val apiDefinitionConfig = ApiDefinitionConfig(apiScope)
  private val customsEnrolmentConfig = CustomsEnrolmentConfig(customsEnrolmentName, eoriIdentifier)
  private val version2 = RequestedVersion("2.0", Some("v2"))

  private val errorResultInternalServer = ErrorResponse(INTERNAL_SERVER_ERROR, errorCode = "INTERNAL_SERVER_ERROR",
    message = "Internal server error").XmlResult.withHeaders("X-Conversation-ID" -> conversationIdValue)

  private val errorResultEoriNotFoundInCustomsEnrolment = ErrorResponse(UNAUTHORIZED, errorCode = "UNAUTHORIZED",
    message = "EORI number not found in Customs Enrolment").XmlResult.withHeaders("X-Conversation-ID" -> conversationIdValue)

  private val errorResultUnauthorised = ErrorResponse(UNAUTHORIZED, errorCode = "UNAUTHORIZED",
    message = "Unauthorised request").XmlResult.withHeaders("X-Conversation-ID" -> conversationIdValue)

  private val errorResultInvalidVersionRequested = ErrorResponse(NOT_ACCEPTABLE, errorCode = "INVALID_VERSION_REQUESTED",
    message = "Invalid API version requested").XmlResult.withHeaders("X-Conversation-ID" -> conversationIdValue)

  private val errorResultBadgeIdentifier = ErrorResponse(BAD_REQUEST, errorCode = BadRequestCode,
    message = "X-Badge-Identifier header is missing or invalid").XmlResult.withHeaders("X-Conversation-ID" -> conversationIdValue)

  private val mockErrorResponse = mock[ErrorResponse]
  private val mockResult = mock[Result]

  private val cspAuthPredicate = Enrolment(apiScope) and AuthProviders(PrivilegedApplication)
  private val nonCspAuthPredicate = Enrolment(customsEnrolmentName) and AuthProviders(GovernmentGateway)
  private val fullIds = ids.copy(maybeRequestedVersion = Some(version2), maybeClientSubscriptionId = None, maybeBadgeIdentifier = Some(badgeIdentifier))

  override protected def beforeEach() {
    reset(mockDeclarationsLogger, mockCustomsConfigService, mockAuthConnector, mockRequestedVersionService,
      mockCustomsDeclarationsBusinessService, mockUuidService)

    when(mockRequestedVersionService.validAcceptHeaders)
      .thenReturn(Set(RequestHeaders.ACCEPT_HMRC_XML_V1_VALUE, RequestHeaders.ACCEPT_HMRC_XML_V2_VALUE))
    when(mockRequestedVersionService.getVersionByAcceptHeader(Some(RequestHeaders.ACCEPT_HMRC_XML_V2_VALUE)))
      .thenReturn(Some(version2))

    when(mockUuidService.uuid()).thenReturn(conversationIdUuid)

    when(mockCustomsConfigService.apiDefinitionConfig).thenReturn(apiDefinitionConfig)
    when(mockCustomsConfigService.customsEnrolmentConfig).thenReturn(customsEnrolmentConfig)

    when(mockCustomsDeclarationsBusinessService.authorisedCspSubmission(any[NodeSeq])(any[HeaderCarrier], any[Ids])).thenReturn(Right(fullIds))
    when(mockCustomsDeclarationsBusinessService.authorisedNonCspSubmission(any[NodeSeq])(any[HeaderCarrier], any[Ids])).thenReturn(Right(fullIds))
  }

  "CustomsDeclarationsController" should {

    "process CSP request when call is authorised for CSP" in {
      authoriseCsp()
      testSubmitResult(ValidRequest) { result =>
        await(result)
        verifyCspAuthorisationCalled(numberOfTimes = 1)
        verifyNonCspAuthorisationCalled(numberOfTimes = 0)
        verify(mockCustomsDeclarationsBusinessService).authorisedCspSubmission(ameq(ValidXML))(any[HeaderCarrier], ameq(fullIds))
        verify(mockCustomsDeclarationsBusinessService, never).authorisedNonCspSubmission(any[NodeSeq])(any[HeaderCarrier], any[Ids])
      }
    }

    "process a non-CSP request when call is unauthorised for CSP but authorised for non-CSP" in {
      authoriseNonCsp(Some(declarantEori))
      testSubmitResult(ValidRequest) { result =>
        await(result)
        verifyCspAuthorisationCalled(numberOfTimes = 1)
        verifyNonCspAuthorisationCalled(numberOfTimes = 1)
        verify(mockCustomsDeclarationsBusinessService, never).authorisedCspSubmission(any[NodeSeq])(any[HeaderCarrier], any[Ids])
        verify(mockCustomsDeclarationsBusinessService).authorisedNonCspSubmission(ameq(ValidXML))(any[HeaderCarrier], ameq(fullIds))
      }
    }

    "respond with status 202 and conversationId in header for a processed valid CSP request" in {
      authoriseCsp()
      testSubmitResult(ValidRequest) { result =>
        status(result) shouldBe ACCEPTED
        header(RequestHeaders.X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
      }
    }

    "respond with status 202 and conversationId in header for a processed valid non-CSP request" in {
      authoriseNonCsp(Some(declarantEori))
      testSubmitResult(ValidRequest) { result =>
        status(result) shouldBe ACCEPTED
        header(RequestHeaders.X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
      }
    }

    "respond with status 202 and conversationId in header for a processed valid non-CSP request without X-BADGE-IDENTIFIER header" in {
      authoriseNonCsp(Some(declarantEori))
      testSubmitResult(ValidRequest.withHeaders((ValidHeaders - X_BADGE_IDENTIFIER_NAME).toSeq: _*)) { result =>
      status(result) shouldBe ACCEPTED
        header(RequestHeaders.X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
      }
    }

    "return result 401 UNAUTHORISED when call is unauthorised for both CSP and non-CSP submissions" in {
      unauthoriseCsp()
      unauthoriseNonCspOnly()
      testSubmitResult(ValidRequest) { result =>
        await(result) shouldBe errorResultUnauthorised
        verifyZeroInteractions(mockCustomsDeclarationsBusinessService)
      }
    }

    "return result 401 UNAUTHORISED when there's no Customs enrolment retrieved for an enrolled non-CSP call" in {
      unauthoriseCsp()
      authoriseNonCspButDontRetrieveCustomsEnrolment()
      testSubmitResult(ValidRequest.fromNonCsp) { result =>
        await(result) shouldBe errorResultEoriNotFoundInCustomsEnrolment
        verifyZeroInteractions(mockCustomsDeclarationsBusinessService)
        PassByNameVerifier(mockDeclarationsLogger, "debug")
          .withByNameParam[String](s"Customs enrolment $customsEnrolmentName not retrieved for authorised non-CSP call with Authorization header=Bearer $nonCspBearerToken")
          .withByNameParam[Ids](fullIds)
          .withAnyHeaderCarrierParam
          .verify()
      }
    }

    "return result 401 UNAUTHORISED when there's no EORI number in Customs enrolment for a non-CSP call" in {
      unauthoriseCsp()
      authoriseNonCsp(maybeEori = None)
      testSubmitResult(ValidRequest) { result =>
        await(result) shouldBe errorResultEoriNotFoundInCustomsEnrolment
        verifyZeroInteractions(mockCustomsDeclarationsBusinessService)
      }
    }

    "return result 406 NOT_ACCEPTABLE when requested version cannot be determined for a CSP request" in {
      when(mockRequestedVersionService.getVersionByAcceptHeader(Some(RequestHeaders.ACCEPT_HMRC_XML_V2_VALUE)))
        .thenReturn(None)

      authoriseCsp()
      testSubmitResult(ValidRequest) { result =>
        await(result) shouldBe errorResultInvalidVersionRequested
        PassByNameVerifier(mockDeclarationsLogger, "error")
          .withByNameParam[String]("Requested version is not valid. Processing failed.")
          .withByNameParam[Ids](Ids(conversationId))
          .withAnyHeaderCarrierParam
          .verify()
      }
    }

    "return result 400 BAD_REQUEST when X-BADGE-IDENTIFIER header is missing for a CSP request" in {
      authoriseCsp()
      testSubmitResult(ValidRequest.copyFakeRequest(headers = ValidRequest.headers.remove(X_BADGE_IDENTIFIER_NAME))) { result =>
        await(result) shouldBe errorResultBadgeIdentifier
        PassByNameVerifier(mockDeclarationsLogger, "error")
         .withByNameParam[String]("Header validation failed because X-Badge-Identifier header is missing or invalid")
         .withByNameParam[Ids](Ids(conversationId, maybeRequestedVersion = Some(version2)))
         .withAnyHeaderCarrierParam
         .verify()
      }
    }

    "return result 400 BAD_REQUEST when X-BADGE-IDENTIFIER header is invalid for a CSP request" in {
      authoriseCsp()
      testSubmitResult(ValidRequest.withHeaders((ValidHeaders + (X_BADGE_IDENTIFIER_NAME -> invalidBadgeIdentifierValue)).toSeq: _*)) { result =>
        await(result) shouldBe errorResultBadgeIdentifier
        PassByNameVerifier(mockDeclarationsLogger, "error")
         .withByNameParam[String]("Header validation failed because X-Badge-Identifier header is missing or invalid ")
         .withByNameParam[Ids](Ids(conversationId))
         .withAnyHeaderCarrierParam
         .verify()
      }
    }

    "return result 406 NOT_ACCEPTABLE when requested version cannot be determined for a non-CSP request" in {
      when(mockRequestedVersionService.getVersionByAcceptHeader(Some(RequestHeaders.ACCEPT_HMRC_XML_V2_VALUE)))
        .thenReturn(None)

      authoriseNonCsp(Some(declarantEori))
      testSubmitResult(ValidRequest) { result =>
        await(result) shouldBe errorResultInvalidVersionRequested
        PassByNameVerifier(mockDeclarationsLogger, "error")
          .withByNameParam[String]("Requested version is not valid. Processing failed.")
          .withByNameParam[Ids](Ids(conversationId))
          .withAnyHeaderCarrierParam
          .verify()

      }
    }

    "respond with status 500 when a CSP request processing fails with a system error" in {
      when(mockCustomsDeclarationsBusinessService.authorisedCspSubmission(any[NodeSeq])(any[HeaderCarrier], any[Ids]))
        .thenReturn(Future.failed(emulatedServiceFailure))

      authoriseCsp()
      testSubmitResult(ValidRequest) { result =>
        await(result) shouldBe errorResultInternalServer
      }
    }

    "respond with status 500 when a non-CSP request processing fails with a system error" in {
      when(mockCustomsDeclarationsBusinessService.authorisedNonCspSubmission(any[NodeSeq])(any[HeaderCarrier], any[Ids]))
        .thenReturn(Future.failed(emulatedServiceFailure))

      authoriseNonCsp(Some(declarantEori))
      testSubmitResult(ValidRequest) { result =>
        await(result) shouldBe errorResultInternalServer
      }
    }

    "return xml-result of the error response returned from CSP request processing" in {
      when(mockCustomsDeclarationsBusinessService.authorisedCspSubmission(any[NodeSeq])(any[HeaderCarrier], any[Ids]))
        .thenReturn(Left(mockErrorResponse))
      when(mockErrorResponse.XmlResult).thenReturn(mockResult)
      when(mockResult.withHeaders(X_CONVERSATION_ID_HEADER)).thenReturn(mockResult)

      authoriseCsp()
      testSubmitResult(ValidRequest) { result =>
        await(result) shouldBe mockResult
      }
    }

    "return xml-result of the error response returned from non-CSP request processing" in {
      when(mockCustomsDeclarationsBusinessService.authorisedNonCspSubmission(any[NodeSeq])(any[HeaderCarrier], any[Ids]))
        .thenReturn(Left(mockErrorResponse))
      when(mockErrorResponse.XmlResult).thenReturn(mockResult)
      when(mockResult.withHeaders(X_CONVERSATION_ID_HEADER)).thenReturn(mockResult)

      authoriseNonCsp(Some(declarantEori))
      testSubmitResult(ValidRequest) { result =>
        await(result) shouldBe mockResult
      }
    }

  }

  private def authoriseCsp(): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(())
  }

  private def unauthoriseCsp(authException: AuthorisationException = new InsufficientEnrolments): Unit = {
    when(mockAuthConnector.authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(authException))
  }

  private def authoriseNonCsp(maybeEori: Option[Eori]): Unit = {
    unauthoriseCsp()
    val customsEnrolment = maybeEori.fold(ifEmpty = Enrolment(customsEnrolmentName)){ eori =>
      Enrolment(customsEnrolmentName).withIdentifier(eoriIdentifier, eori.value)
    }
    when(mockAuthConnector.authorise(ameq(nonCspAuthPredicate), ameq(Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Enrolments(Set(customsEnrolment)))
  }

  private def authoriseNonCspButDontRetrieveCustomsEnrolment(): Unit = {
    unauthoriseCsp()
    when(mockAuthConnector.authorise(ameq(nonCspAuthPredicate), ameq(Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Enrolments(Set.empty))
  }

  private def unauthoriseNonCspOnly(authException: AuthorisationException = new InsufficientEnrolments): Unit = {
    when(mockAuthConnector.authorise(ameq(nonCspAuthPredicate), ameq(Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(authException))
  }

  private def verifyCspAuthorisationCalled(numberOfTimes: Int) = {
    verify(mockAuthConnector, times(numberOfTimes))
      .authorise(ameq(cspAuthPredicate), ameq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext])
  }

  private def verifyNonCspAuthorisationCalled(numberOfTimes: Int) = {
    verify(mockAuthConnector, times(numberOfTimes))
      .authorise(ameq(nonCspAuthPredicate), ameq(Retrievals.authorisedEnrolments))(any[HeaderCarrier], any[ExecutionContext])
  }

  private def testSubmitResult(request: Request[AnyContent])(test: Future[Result] => Unit) {
    val result = controller.submit().apply(request)
    test(result)
  }

}
