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

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.http.Status
import play.api.mvc._
import play.api.test.Helpers.{UNAUTHORIZED, header, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.{DeclarationStatusConnector, GoogleAnalyticsConnector}
import uk.gov.hmrc.customs.declaration.controllers.DeclarationStatusController
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{ValidateAndExtractHeadersStatusAction, _}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedStatusRequest, HasAnalyticsValues, HasConversationId}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import util.AuthConnectorNrsDisabledStubbing
import util.FakeRequests._
import util.RequestHeaders._
import util.TestData._
import util.TestXMLData.validStatusResponse

import scala.concurrent.Future
import scala.xml.NodeSeq

class CustomsDeclarationStatusControllerSpec extends UnitSpec
  with Matchers with MockitoSugar with BeforeAndAfterEach {

  trait SetUp extends AuthConnectorNrsDisabledStubbing {
    override val mockAuthConnector: AuthConnector = mock[AuthConnector]

    protected val mockStatusResponseFilterService: StatusResponseFilterService = mock[StatusResponseFilterService]
    protected val mockDeclarationsLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockCdsLogger: CdsLogger = mock[CdsLogger]
    protected val mockErrorResponse: ErrorResponse = mock[ErrorResponse]
    protected val mockResult: Result = mock[Result]
    protected val mockGoogleAnalyticsConnector: GoogleAnalyticsConnector = mock[GoogleAnalyticsConnector]
    protected val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]

    protected val stubHttpResponse = HttpResponse(responseStatus = Status.OK, responseJson = None, responseString = Some(validStatusResponse().toString))

    protected val endpointAction = new EndpointAction {
      override val logger: DeclarationsLogger = mockDeclarationsLogger
      override val googleAnalyticsValues: GoogleAnalyticsValues = GoogleAnalyticsValues.Submit
      override val correlationIdService: UniqueIdsService = stubUniqueIdsService
    }

    protected val mockStatusConnector: DeclarationStatusConnector = mock[DeclarationStatusConnector]
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]
    protected val dateTime = new DateTime()

    protected val stubAuthStatusAction: AuthStatusAction = new AuthStatusAction (mockAuthConnector, mockDeclarationsLogger)
    protected val stubValidateAndExtractHeadersStatusAction: ValidateAndExtractHeadersStatusAction = new ValidateAndExtractHeadersStatusAction(new HeaderStatusValidator(mockDeclarationsLogger), mockDeclarationsLogger, mockGoogleAnalyticsConnector)
    protected val stubDeclarationStatusService = new DeclarationStatusService(mockStatusResponseFilterService, mockDeclarationsLogger, mockStatusConnector, mockDateTimeService, stubUniqueIdsService)
    protected val stubDeclarationStatusValuesAction = new DeclarationStatusValuesAction(mockDeclarationsLogger, stubUniqueIdsService)

    protected val controller: DeclarationStatusController = new DeclarationStatusController(
      stubValidateAndExtractHeadersStatusAction,
      stubAuthStatusAction,
      stubDeclarationStatusValuesAction,
      stubDeclarationStatusService,
      mockDeclarationsLogger,
      mockGoogleAnalyticsConnector) {}

    protected def awaitSubmit(request: Request[AnyContent]): Result = {
      controller.get(mrnValue).apply(request)
    }

    protected def submit(request: Request[AnyContent]): Future[Result] = {
      controller.get(mrnValue).apply(request)
    }

    when(mockStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], meq[UUID](dmirId.uuid).asInstanceOf[DeclarationManagementInformationRequestId], any[ApiVersion], meq[String](mrn.value).asInstanceOf[Mrn])(any[AuthorisedStatusRequest[_]])).thenReturn(Future.successful(stubHttpResponse))
    when(mockDateTimeService.nowUtc()).thenReturn(dateTime)
    when(mockDeclarationConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
    when(mockStatusResponseFilterService.transform(any[NodeSeq])).thenReturn(<xml>some xml</xml>)
  }

  private val errorResultBadgeIdentifier = errorBadRequest("X-Badge-Identifier header is missing or invalid").XmlResult.withHeaders(X_CONVERSATION_ID_HEADER)

  "CustomsDeclarationStatusController" should {
    "process CSP request when call is authorised for CSP" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmit(ValidDeclarationStatusRequest)

      verifyCspAuthorisationCalled(numberOfTimes = 1)
      verifyNonCspAuthorisationCalled(numberOfTimes = 0)
    }

    "respond with status 200 and conversationId in header for a processed valid CSP request" in new SetUp() {
      authoriseCsp()

      val result: Future[Result] = submit(ValidDeclarationStatusRequest)

      status(result) shouldBe OK
      header(X_CONVERSATION_ID_NAME, result) shouldBe Some(conversationIdValue)
      verify(mockGoogleAnalyticsConnector).success(any[HasConversationId with HasAnalyticsValues])
    }

    "respond with status 400 for a CSP request with a missing X-Badge-Identifier" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmit(ValidDeclarationStatusRequest.copyFakeRequest(headers = ValidDeclarationStatusRequest.headers.remove(X_BADGE_IDENTIFIER_NAME)))
      result shouldBe errorResultBadgeIdentifier
      verifyZeroInteractions(mockStatusConnector)
    }

    "respond with status 500 for a request with a missing X-Client-ID" in new SetUp() {
      authoriseCsp()

      val result: Result = awaitSubmit(ValidDeclarationStatusRequest.copyFakeRequest(headers = ValidDeclarationStatusRequest.headers.remove(X_CLIENT_ID_NAME)))
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
      when(mockStatusConnector.send(any[DateTime],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        meq[UUID](dmirId.uuid).asInstanceOf[DeclarationManagementInformationRequestId],
        any[ApiVersion], meq[String](mrn.value).asInstanceOf[Mrn])(any[AuthorisedStatusRequest[_]]))
        .thenReturn(Future.failed(emulatedServiceFailure))

      authoriseCsp()

      val result: Result = awaitSubmit(ValidDeclarationStatusRequest)

      result.header.status shouldBe INTERNAL_SERVER_ERROR
      verifyZeroInteractions(mockGoogleAnalyticsConnector)
    }
  }
}
