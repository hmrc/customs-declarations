/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Assertion, BeforeAndAfterEach}
import play.api.mvc._
import play.api.test.Helpers
import play.api.test.Helpers.{UNAUTHORIZED, header, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.declaration.connectors.CustomsDeclarationsMetricsConnector
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.controllers.{Common, CustomsDeclarationController, ErrorResponse}
import uk.gov.hmrc.customs.declaration.logging.{CdsLogger, DeclarationsLogger}
import uk.gov.hmrc.customs.declaration.model.CustomsDeclarationsMetricsRequest
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{HasConversationId, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.http.HeaderCarrier
import util.AuthConnectorStubbing
import util.CustomsDeclarationsMetricsTestData._
import util.FakeRequests._
import util.RequestHeaders._
import util.TestData._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class CustomsDeclarationControllerSpec extends AnyWordSpecLike
  with Matchers with BeforeAndAfterEach{

  trait SetUp extends AuthConnectorStubbing {
    override val mockAuthConnector: AuthConnector = mock[AuthConnector]

    private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    protected implicit val conversationIdRequest: HasConversationId = TestConversationIdRequest

    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockCdsLogger: CdsLogger = mock[CdsLogger]
    protected val mockBusinessService: StandardDeclarationSubmissionService = mock[StandardDeclarationSubmissionService]
    protected val mockErrorResponse: ErrorResponse = mock[ErrorResponse]
    protected val mockResult: Result = mock[Result]
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]
    protected val mockMetricsConnector: CustomsDeclarationsMetricsConnector = mock[CustomsDeclarationsMetricsConnector]
    protected val mockXmlValidationService: XmlValidationService = mock[XmlValidationService]
    protected val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]

    protected val conversationIdAction = new ConversationIdAction(mockLogger, stubUniqueIdsService, mockDateTimeService)

    protected val customsAuthService = new CustomsAuthService(mockAuthConnector, mockLogger)
    protected val headerValidator = new HeaderWithContentTypeValidator(mockLogger)
    protected val stubShutterCheckAction = new ShutterCheckAction(mockLogger, mockDeclarationConfigService)
    protected val stubAuthAction: AuthAction = new AuthAction(customsAuthService, headerValidator, mockLogger, mockDeclarationConfigService)
    protected val stubValidateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(new HeaderWithContentTypeValidator(mockLogger))
    
    protected val stubPayloadValidationAction: PayloadValidationAction = new PayloadValidationAction(mockXmlValidationService, mockLogger) {
      override def executionContext: ExecutionContext = ec
    }

    protected val common = new Common(stubShutterCheckAction, stubAuthAction, stubValidateAndExtractHeadersAction, mockLogger, Helpers.stubControllerComponents())

    protected val controller: CustomsDeclarationController = new CustomsDeclarationController(common, mockBusinessService, stubPayloadValidationAction, conversationIdAction, Some(mockMetricsConnector)) {}
    protected val controllerWithoutMetrics: CustomsDeclarationController = new CustomsDeclarationController(common, mockBusinessService, stubPayloadValidationAction, conversationIdAction, None) {}

    protected def awaitSubmit(request: Request[AnyContent]): Result = {
      await(controller.post().apply(request))
    }

    protected def submit(request: Request[AnyContent]): Future[Result] = {
      controller.post().apply(request)
    }

    protected def verifyMetrics: Assertion = {
      val captor: ArgumentCaptor[CustomsDeclarationsMetricsRequest] = ArgumentCaptor.forClass(classOf[CustomsDeclarationsMetricsRequest])
      verify(mockMetricsConnector).post(captor.capture())(any[HasConversationId])
      captor.getValue.eventType shouldBe "DECLARATION"
      captor.getValue.conversationId shouldBe conversationId
      captor.getValue.eventStart shouldBe EventStart
      captor.getValue.eventEnd shouldBe EventEnd
    }

    when(mockXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext])).thenReturn(Future.successful(()))
    when(mockBusinessService.send(any[ValidatedPayloadRequest[Any]], any[HeaderCarrier])).thenReturn(Future.successful(Right(Some(nrSubmissionId))))
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
    when(mockDeclarationConfigService.declarationsShutterConfig).thenReturn(allVersionsUnshuttered)
  }

  private val errorResultEoriNotFoundInCustomsEnrolment = ErrorResponse(UNAUTHORIZED, errorCode = "UNAUTHORIZED",
    message = "EORI number not found in Customs Enrolment").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  private val errorResultUnauthorised = ErrorResponse(UNAUTHORIZED, errorCode = "UNAUTHORIZED",
    message = "Unauthorised request").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  private val errorResultBadgeIdentifier = errorBadRequest("X-Badge-Identifier header is missing or invalid").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  "CustomsDeclarationController" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      authoriseCsp()

      awaitSubmit(ValidSubmissionV2Request)

      verifyCspAuthorisationCalled(numberOfTimes = 1)
      verifyNonCspAuthorisationCalled(numberOfTimes = 0)
    }

    "process a non-CSP request when call is unauthorised for CSP but authorised for non-CSP" in new SetUp() {
      authoriseNonCsp(Some(declarantEori))

      awaitSubmit(ValidSubmissionV2Request)

      verifyCspAuthorisationCalled(numberOfTimes = 1)
      verifyNonCspAuthorisationCalled(numberOfTimes = 1)
    }

    "log Metrics when configured" in new SetUp() {
      authoriseCsp()
      when(mockDateTimeService.zonedDateTimeUtc).thenReturn(EventStart, EventEnd)

      val result: Future[Result] = submit(ValidSubmissionV2Request)

      status(result) shouldBe ACCEPTED
      verifyMetrics
    }

    "NOT log Metrics when not configured" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = controllerWithoutMetrics.post().apply(ValidSubmissionV2Request)

      status(result) shouldBe ACCEPTED

      verifyNoMoreInteractions(mockMetricsConnector)
    }

    "respond with status 202 and ContentType application.xml and conversationId in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submit(ValidSubmissionV2Request)

      status(result) shouldBe ACCEPTED
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 202 and ContentType with utf-8 in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submit(ValidSubmissionV2RequestWithCharSet)

      status(result) shouldBe ACCEPTED
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 415 and ContentType with utf-16 in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submit(ValidSubmissionV2RequestWithInvalidCharSet)

      status(result) shouldBe UNSUPPORTED_MEDIA_TYPE
    }



    "respond with status 400 for a CSP request with a missing X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmit(ValidSubmissionV2Request.withHeaders(ValidSubmissionV2Request.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      result shouldBe errorResultBadgeIdentifier
      verifyNoMoreInteractions(mockBusinessService)
      verifyNoMoreInteractions(mockXmlValidationService)
    }

    "respond with status 500 for a request with a missing X-Client-ID" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submit(ValidSubmissionV2Request.withHeaders(ValidSubmissionV2Request.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyNoMoreInteractions(mockBusinessService)
      verifyNoMoreInteractions(mockXmlValidationService)
    }

    "respond with status 400 for a request with an invalid X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmit(ValidSubmissionV2Request.withHeaders((ValidHeadersV2 + X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS).toSeq: _*))

      result shouldBe errorResultBadgeIdentifier
      verifyNoMoreInteractions(mockBusinessService)
      verifyNoMoreInteractions(mockXmlValidationService)
    }

    "respond with status 202 and conversationId in header for a processed valid non-CSP request" in new SetUp() {
      authoriseNonCsp(Some(declarantEori))

      val result: Future[Result] = submit(ValidSubmissionV2Request)

      status(result) shouldBe ACCEPTED
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "return result 401 UNAUTHORISED and conversationId in header when call is unauthorised for both CSP and non-CSP submissions" in new SetUp() {
      unauthoriseCsp()
      unauthoriseNonCspOnly()

      val result: Future[Result] = submit(ValidSubmissionV2Request)

      await(result) shouldBe errorResultUnauthorised
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
      verifyNoMoreInteractions(mockBusinessService)
      verifyNoMoreInteractions(mockXmlValidationService)
    }

    "return result 401 UNAUTHORISED and conversationId in header when there's no Customs enrolment retrieved for an enrolled non-CSP call" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCspButDontRetrieveCustomsEnrolment()

      val result: Future[Result] = submit(ValidSubmissionV2Request.fromNonCsp)

      await(result) shouldBe errorResultEoriNotFoundInCustomsEnrolment
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
      verifyNoMoreInteractions(mockBusinessService)
      verifyNoMoreInteractions(mockXmlValidationService)
    }

    "return result 401 UNAUTHORISED and conversationId in header when there's no EORI number in Customs enrolment for a non-CSP call" in new SetUp() {
      unauthoriseCsp()
      authoriseNonCsp(maybeEori = None)

      val result: Future[Result] = submit(ValidSubmissionV2Request)

      await(result) shouldBe errorResultEoriNotFoundInCustomsEnrolment
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
      verifyNoMoreInteractions(mockBusinessService)
      verifyNoMoreInteractions(mockXmlValidationService)
    }

    "return the error response returned from the Communication service" in new SetUp() {
      when(mockBusinessService.send(any[ValidatedPayloadRequest[Any]], any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(mockResult)))
      authoriseCsp()

      val result: Result = awaitSubmit(ValidSubmissionV2Request)

      result shouldBe mockResult
    }

    "return the Internal Server error when business service returns a 500 " in new SetUp() {
      when(mockBusinessService.send(any[ValidatedPayloadRequest[Any]], any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorResponse.ErrorInternalServerError.XmlResult)))
      authoriseCsp()

      val result: Result = awaitSubmit(ValidSubmissionV2Request)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
