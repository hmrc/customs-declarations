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

package unit.controllers

import java.util.UUID
import org.joda.time.DateTime
import play.api.test.Helpers.{redirectLocation, status, _}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.mvc._
import play.api.test.Helpers
import play.api.test.Helpers.{UNAUTHORIZED, header, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector}
import uk.gov.hmrc.customs.declaration.controllers.DeclarationStatusController
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{ValidateAndExtractHeadersStatusAction, _}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.FakeRequests._
import util.RequestHeaders._
import util.TestData._
import util.{AuthConnectorNrsDisabledStubbing, StatusTestXMLData}
import scala.concurrent.Future
import scala.xml.NodeSeq

class CustomsDeclarationStatusControllerSpec extends AnyWordSpecLike with Matchers with MockitoSugar{

  trait SetUp extends AuthConnectorNrsDisabledStubbing {
    override val mockAuthConnector: AuthConnector = mock[AuthConnector]

    protected implicit val ec = Helpers.stubControllerComponents().executionContext
    protected val mockStatusResponseFilterService: StatusResponseFilterService = mock[StatusResponseFilterService]
    protected val mockStatusResponseValidationService: StatusResponseValidationService = mock[StatusResponseValidationService]
    protected val mockMdgPayloadDecorator: MdgPayloadDecorator = mock[MdgPayloadDecorator]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockDeclarationsLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockCdsLogger: CdsLogger = mock[CdsLogger]
    protected val mockErrorResponse: ErrorResponse = mock[ErrorResponse]
    protected val mockResult: Result = mock[Result]
    protected val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]

    protected val stubHttpResponse = HttpResponse(Status.OK, StatusTestXMLData.generateDeclarationStatusResponse().toString)

    protected val mockStatusConnector: DeclarationStatusConnector = mock[DeclarationStatusConnector]
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]
    protected val dateTime = new DateTime()

    protected val stubShutterCheckAction = new ShutterCheckAction(mockDeclarationsLogger, mockDeclarationConfigService)
    protected val stubAuthStatusAction: AuthStatusAction = new AuthStatusAction (mockAuthConnector, mockDeclarationsLogger)
    protected val stubValidateAndExtractHeadersStatusAction: ValidateAndExtractHeadersStatusAction = new ValidateAndExtractHeadersStatusAction(new HeaderStatusValidator(mockDeclarationsLogger), mockDeclarationsLogger)
    protected val stubDeclarationStatusService = new DeclarationStatusService(mockDeclarationsLogger, mockApiSubscriptionFieldsConnector, mockStatusConnector, mockMdgPayloadDecorator, mockDateTimeService, stubUniqueIdsService, mockStatusResponseFilterService, mockStatusResponseValidationService)
    protected val stubConversationIdAction = new ConversationIdAction(mockDeclarationsLogger, stubUniqueIdsService, mockDateTimeService)

    protected val controller: DeclarationStatusController = new DeclarationStatusController(
      stubShutterCheckAction,
      stubValidateAndExtractHeadersStatusAction,
      stubAuthStatusAction,
      stubConversationIdAction,
      stubDeclarationStatusService,
      Helpers.stubControllerComponents(),
      mockDeclarationsLogger) {}

    protected def awaitSubmit(request: Request[AnyContent]): Result = {
      await(controller.get(mrnValue).apply(request))
    }

    protected def submit(request: Request[AnyContent]): Future[Result] = {
      controller.get(mrnValue).apply(request)
    }

    when(mockStatusConnector.send(any[NodeSeq], any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], any[ApiVersion])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
    when(mockDateTimeService.nowUtc()).thenReturn(dateTime)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
    when(mockDeclarationConfigService.declarationsShutterConfig).thenReturn(allVersionsUnshuttered)
    when(mockStatusResponseFilterService.transform(any[NodeSeq])).thenReturn(<xml>some xml</xml>)
    when(mockStatusResponseValidationService.validate(any[NodeSeq],meq(validBadgeIdentifierValue).asInstanceOf[BadgeIdentifier])).thenReturn(Right(true))
  }

  private val errorResultBadgeIdentifier = errorBadRequest("X-Badge-Identifier header is missing or invalid").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  "CustomsDeclarationStatusController" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      authoriseCsp()

      awaitSubmit(ValidDeclarationStatusRequest)

      verifyCspAuthorisationCalled(numberOfTimes = 1)
      verifyNonCspAuthorisationCalled(numberOfTimes = 0)
    }

    "respond with status 200 and conversationId in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submit(ValidDeclarationStatusRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
    }

    "respond with status 400 for a CSP request with a missing X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmit(ValidDeclarationStatusRequest.withHeaders(ValidDeclarationStatusRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      result shouldBe errorResultBadgeIdentifier
      verifyZeroInteractions(mockStatusConnector)
    }

    "respond with status 500 for a request with a missing X-Client-ID" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submit(ValidDeclarationStatusRequest.withHeaders(ValidDeclarationStatusRequest.headers.remove(X_CLIENT_ID_NAME)))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyZeroInteractions(mockStatusConnector)
    }
    "respond with status 400 for a request with an invalid X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmit(ValidDeclarationStatusRequest.withHeaders((ValidHeadersV2 + X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS).toSeq: _*))

      result shouldBe errorResultBadgeIdentifier
      verifyZeroInteractions(mockStatusConnector)
    }

    "respond with status 401 and conversationId in header when non-CSP request" in new SetUp() {
      authoriseNonCsp(Some(declarantEori))

      val result: Future[Result] = submit(ValidDeclarationStatusRequest)

      status(result) shouldBe UNAUTHORIZED
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)

    }

    "return the Internal Server error when connector returns a 500 " in new SetUp() {
      when(mockStatusConnector.send(any[NodeSeq],
        any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion])(any[AuthorisedRequest[_]]))
        .thenReturn(Future.failed(emulatedServiceFailure))

      authoriseCsp()

      val result: Result = awaitSubmit(ValidDeclarationStatusRequest)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
