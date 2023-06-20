/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.services

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{FORBIDDEN, NOT_FOUND}
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector}
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.StatusTestXMLData.expectedDeclarationStatusPayload
import util.TestData._

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class DeclarationStatusServiceSpec extends AnyWordSpecLike with MockitoSugar with BeforeAndAfterEach with Matchers{
  private val dateTime = Instant.now()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val ar: AuthorisedRequest[AnyContentAsXml] = util.TestData.TestAuthorisedStatusRequest

  protected lazy val mockStatusResponseFilterService: StatusResponseFilterService = mock[StatusResponseFilterService]
  protected lazy val mockStatusResponseValidationService: StatusResponseValidationService = mock[StatusResponseValidationService]
  protected lazy val mockMdgPayloadDecorator: MdgPayloadDecorator = mock[MdgPayloadDecorator]
  protected lazy val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
  protected lazy val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
  protected lazy val mockDeclarationStatusConnector: DeclarationStatusConnector = mock[DeclarationStatusConnector]
  protected lazy val mockPayloadDecorator: MdgPayloadDecorator = mock[MdgPayloadDecorator]
  protected lazy val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
  protected lazy val mockHttpResponse: HttpResponse = mock[HttpResponse]
  protected lazy val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
  protected val mrn: Mrn = Mrn("theMrn")

  trait SetUp {
    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockDeclarationStatusConnector.send(any[NodeSeq], any[Instant], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
      any[ApiVersion])(any[AuthorisedRequest[_]])).thenReturn(Future.successful(mockHttpResponse))
    when(mockHttpResponse.body).thenReturn("<xml>some xml</xml>")
    when(mockHttpResponse.headers).thenReturn(any[Map[String, Seq[String]]])
    when(mockStatusResponseFilterService.transform(<xml>backendXml</xml>)).thenReturn(<xml>transformed</xml>)
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))

    protected lazy val service: DeclarationStatusService = new DeclarationStatusService(mockLogger, mockApiSubscriptionFieldsConnector, mockDeclarationStatusConnector, mockMdgPayloadDecorator,
      mockDateTimeProvider, stubUniqueIdsService, mockStatusResponseFilterService, mockStatusResponseValidationService)

    protected def send(vpr: AuthorisedRequest[AnyContentAsXml] = TestAuthorisedStatusRequest, hc: HeaderCarrier = headerCarrier): Either[Result, HttpResponse] = {
     (service.send(mrn) (vpr, hc).futureValue)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockMdgPayloadDecorator, mockDateTimeProvider, mockDeclarationStatusConnector, mockHttpResponse, mockStatusResponseFilterService, mockStatusResponseValidationService)
  }
  "BusinessService" should {

    "send xml to connector" in new SetUp() {
      when(mockStatusResponseValidationService.validate(any[NodeSeq], meq(validBadgeIdentifierValue).asInstanceOf[BadgeIdentifier])).thenReturn(Right(true))
      when(mockMdgPayloadDecorator.status(meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        meq(dateTime),
        meq[String](mrn.value).asInstanceOf[Mrn],
        meq[UUID](dmirId.uuid).asInstanceOf[DeclarationManagementInformationRequestId],
        any[ApiSubscriptionFieldsResponse])
        (any[AuthorisedRequest[_]])).thenReturn(expectedDeclarationStatusPayload)

      val result: Either[Result, HttpResponse] = send()
      result.toOption.get.body shouldBe "<xml>transformed</xml>"
      verify(mockDeclarationStatusConnector).send(expectedDeclarationStatusPayload, dateTime, correlationId, VersionOne)(TestAuthorisedStatusRequest)
    }

    "return 404 error response when MDG call fails with 404" in new SetUp() {
      when(mockDeclarationStatusConnector.send(any[NodeSeq],
        any[Instant],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion])(any[AuthorisedRequest[_]])).thenReturn(Future.failed(new Non2xxResponseException(NOT_FOUND)))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse.ErrorNotFound.XmlResult.withConversationId)
    }

    "return 500 error response when MDG call fails" in new SetUp() {
      when(mockDeclarationStatusConnector.send(any[NodeSeq],
        any[Instant],
        meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId],
        any[ApiVersion])(any[AuthorisedRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }

    "return 400 when validationService fails validation" in new SetUp() {

      val result: Either[Result, HttpResponse] = send()
      result shouldBe Left(ErrorResponse.ErrorGenericBadRequest.XmlResult.withConversationId)

    }
  }
}
