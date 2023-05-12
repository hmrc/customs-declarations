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

import akka.actor.ActorSystem
import akka.pattern.CircuitBreakerOpenException
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.FORBIDDEN
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, errorInternalServerError}
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, DeclarationConnector}
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, HasConversationId, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.ApiSubscriptionFieldsTestData._
import util.CustomsDeclarationsMetricsTestData
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData._

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class DeclarationServiceSpec extends AnyWordSpecLike with MockitoSugar with Matchers {
  private val dateTime = Instant.now()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val expectedApiSubscriptionKey = ApiSubscriptionKey(clientId, "customs%2Fdeclarations", VersionOne)
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest
  private val wrappedValidXML = <wrapped></wrapped>
  private val errorResponseServiceUnavailable = errorInternalServerError("This service is currently unavailable")
  private val errorResponseMissingEori = errorInternalServerError("Missing authenticated eori in service lookup")

  trait SetUp {
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockMdgDeclarationConnector: DeclarationConnector = mock[DeclarationConnector]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockPayloadDecorator: MdgPayloadDecorator = mock[MdgPayloadDecorator]
    protected val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
    protected val mockHttpResponse: HttpResponse = mock[HttpResponse]
    protected val mockNrsService: NrsService = mock[NrsService]
    protected val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    protected val mockDeclarationsConfig: DeclarationsConfig = mock[DeclarationsConfig]

    protected lazy val service: DeclarationService = new DeclarationService {
      val logger: DeclarationsLogger = mockLogger
      val connector: DeclarationConnector = mockMdgDeclarationConnector
      val apiSubFieldsConnector: ApiSubscriptionFieldsConnector = mockApiSubscriptionFieldsConnector
      val wrapper: MdgPayloadDecorator = mockPayloadDecorator
      val dateTimeProvider: DateTimeService = mockDateTimeProvider
      val uniqueIdsService: UniqueIdsService = stubUniqueIdsService
      val nrsService: NrsService = mockNrsService
      val declarationsConfigService: DeclarationsConfigService = mockDeclarationsConfigService
      val actorSystem: ActorSystem = ActorSystem("DeclarationServiceSpec")
      override implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    }

    protected def send(vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): Either[Result, Option[NrSubmissionId]] = {
      (service.send(vpr, hc)).futureValue
    }

    when(mockPayloadDecorator.wrap(meq(TestXmlPayload), any[ApiSubscriptionFieldsResponse](), any[Instant])(any[ValidatedPayloadRequest[_]])).thenReturn(wrappedValidXML)
    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockDateTimeProvider.zonedDateTimeUtc).thenReturn(CustomsDeclarationsMetricsTestData.EventStart, CustomsDeclarationsMetricsTestData.EventEnd)
    when(mockMdgDeclarationConnector.send(any[NodeSeq], meq(dateTime), any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.successful(mockHttpResponse))
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
    when(mockNrsService.send(vpr)).thenReturn(Future.successful(nrSubmissionId))
    when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
    when(mockDeclarationsConfigService.declarationsConfig).thenReturn(mockDeclarationsConfig)
  }

  "BusinessService" should {

    "send transformed xml to connector" in new SetUp() {

      val result: Either[Result, Option[NrSubmissionId]] = send()
      result shouldBe Right(Some(nrSubmissionId))
      verify(mockMdgDeclarationConnector).send(meq(wrappedValidXML), any[Instant], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])

      PassByNameVerifier(mockLogger, "info")
        .withByNameParam[String]("Duration of call to NRS 2000 ms")
        .withParamMatcher(any[HasConversationId])
        .verify()
    }

    "send transformed xml to connector even when nrs fails" in new SetUp() {
      when(mockNrsService.send(vpr)).thenReturn(Future.failed(emulatedServiceFailure))

      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Right(Some(nrSubmissionId))
      verify(mockMdgDeclarationConnector).send(meq(wrappedValidXML), any[Instant], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])
    }

  }

  "get utc date time and pass to connector" in new SetUp() {

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Right(Some(nrSubmissionId))
    verify(mockMdgDeclarationConnector).send(any[NodeSeq], meq(dateTime), any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])
  }

  "pass in version to connector" in new SetUp() {

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Right(Some(nrSubmissionId))
    verify(mockMdgDeclarationConnector).send(any[NodeSeq], any[Instant], any[UUID], meq(VersionOne))(any[ValidatedPayloadRequest[_]])
  }

  "call payload decorator passing incoming xml" in new SetUp() {

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Right(Some(nrSubmissionId))
    verify(mockPayloadDecorator).wrap(meq(TestXmlPayload), any[ApiSubscriptionFieldsResponse](), any[Instant])(any[ValidatedPayloadRequest[_]])
    verify(mockApiSubscriptionFieldsConnector).getSubscriptionFields(meq(expectedApiSubscriptionKey))(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
  }

  "return 500 error response when subscription fields call fails" in new SetUp() {
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Left(ErrorInternalServerError.XmlResult.withConversationId)
    verifyNoMoreInteractions(mockPayloadDecorator)
    verifyNoMoreInteractions(mockMdgDeclarationConnector)
  }

  "return 500 error response when authenticatedEori is blank" in new SetUp() {
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier]))
      .thenReturn(Future.successful(apiSubscriptionFieldsResponse.copy(fields = ApiSubscriptionFieldsResponseFields(Some("")))))

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Left(errorResponseMissingEori.XmlResult.withConversationId)
    verifyNoMoreInteractions(mockPayloadDecorator)
    verifyNoMoreInteractions(mockMdgDeclarationConnector)
  }

  "return 500 error response when MDG call fails" in new SetUp() {
    when(mockMdgDeclarationConnector.send(any[NodeSeq], any[Instant], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Left(ErrorInternalServerError.XmlResult.withConversationId.withNrSubmissionId(nrSubmissionId))
  }

  "return 500 error response when MDG circuit breaker trips" in new SetUp() {
    when(mockMdgDeclarationConnector.send(any[NodeSeq], any[Instant], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(new CircuitBreakerOpenException(FiniteDuration(10, TimeUnit.SECONDS))))

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Left(errorResponseServiceUnavailable.XmlResult.withNrSubmissionId(nrSubmissionId).withConversationId)
  }

  "when NRS disabled should not get a submission id" in new SetUp() {

    when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigDisabled)

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Right(None)
    verify(mockMdgDeclarationConnector).send(meq(wrappedValidXML), any[Instant], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])
  }

  "return 500 error when CSP has no authenticatedEori in api subscription fields" in new SetUp() {
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier]))
      .thenReturn(Future.successful(apiSubscriptionFieldsResponse.copy(fields = ApiSubscriptionFieldsResponseFields(None))))

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Left(errorResponseMissingEori.XmlResult.withConversationId)
    verifyNoMoreInteractions(mockPayloadDecorator)
    verifyNoMoreInteractions(mockMdgDeclarationConnector)
  }


  "return 403 error response when EIS call fails with 403 and payloadForbiddenEnabled is on" in new SetUp() {
    when(mockDeclarationsConfig.payloadForbiddenEnabled).thenReturn(true)
    when(mockMdgDeclarationConnector.send(any[NodeSeq], any[Instant], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]]))
      .thenReturn(Future.failed(new Non2xxResponseException(FORBIDDEN)))
    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Left(ErrorResponse.ErrorPayloadForbidden.XmlResult.withConversationId.withNrSubmissionId(nrSubmissionId))
  }

  "return 500 error response when EIS call fails with 403 but payloadForbiddenEnabled is off" in new SetUp() {
    when(mockDeclarationsConfig.payloadForbiddenEnabled).thenReturn(false)
    when(mockMdgDeclarationConnector.send(any[NodeSeq], any[Instant], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]]))
      .thenReturn(Future.failed(new Non2xxResponseException(FORBIDDEN)))
    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId.withNrSubmissionId(nrSubmissionId))
  }
}
