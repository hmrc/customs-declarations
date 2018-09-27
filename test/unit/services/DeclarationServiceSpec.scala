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

package unit.services

import java.util.UUID

import akka.actor.ActorSystem
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{verify, verifyZeroInteractions, when}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorInternalServerError
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, MdgDeclarationConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

class DeclarationServiceSpec extends UnitSpec with MockitoSugar {
  private val dateTime = new DateTime()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private val expectedApiSubscriptionKey = ApiSubscriptionKey(clientId, "customs%2Fdeclarations", VersionOne)
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest
  private val wrappedValidXML = <wrapped></wrapped>
  private val errorResponseServiceUnavailable = errorInternalServerError("This service is currently unavailable")

  trait SetUp {
    protected val mockLogger: DeclarationsLogger = stubDeclarationsLogger
    protected val mockMdgDeclarationConnector: MdgDeclarationConnector = mock[MdgDeclarationConnector]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockPayloadDecorator: MdgPayloadDecorator = mock[MdgPayloadDecorator]
    protected val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
    protected val mockHttpResponse: HttpResponse = mock[HttpResponse]
    protected val mockNrsService: NrsService = mock[NrsService]
    protected val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]

    protected lazy val service: DeclarationService = new DeclarationService {
      val logger: DeclarationsLogger = mockLogger
      val connector: MdgDeclarationConnector = mockMdgDeclarationConnector
      val apiSubFieldsConnector: ApiSubscriptionFieldsConnector = mockApiSubscriptionFieldsConnector
      val wrapper: MdgPayloadDecorator = mockPayloadDecorator
      val dateTimeProvider: DateTimeService = mockDateTimeProvider
      val uniqueIdsService: UniqueIdsService = stubUniqueIdsService
      val nrsService: NrsService = mockNrsService
      val declarationsConfigService: DeclarationsConfigService = mockDeclarationsConfigService
      val actorSystem: ActorSystem = ActorSystem("DeclarationServiceSpec")
    }

    protected def send(vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): Either[Result, Option[NrSubmissionId]] = {
      await(service.send(vpr, hc))
    }

    when(mockPayloadDecorator.wrap(meq(TestXmlPayload), meq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId], any[DateTime])(any[ValidatedPayloadRequest[_]])).thenReturn(wrappedValidXML)
    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockMdgDeclarationConnector.send(any[NodeSeq], meq(dateTime), any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.successful(mockHttpResponse))
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
    when(mockNrsService.send(vpr, headerCarrier)).thenReturn(Future.successful(nrSubmissionId))
    when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
  }
    "BusinessService" should {

      "send transformed xml to connector" in new SetUp() {

        val result: Either[Result, Option[NrSubmissionId]] = send()

        result shouldBe Right(Some(nrSubmissionId))
        verify(mockMdgDeclarationConnector).send(meq(wrappedValidXML), any[DateTime], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])
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
      verify(mockMdgDeclarationConnector).send(any[NodeSeq], any[DateTime], any[UUID], meq(VersionOne))(any[ValidatedPayloadRequest[_]])
    }

    "call payload decorator passing incoming xml" in new SetUp() {

      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Right(Some(nrSubmissionId))
      verify(mockPayloadDecorator).wrap(meq(TestXmlPayload), meq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId], any[DateTime])(any[ValidatedPayloadRequest[_]])
      verify(mockApiSubscriptionFieldsConnector).getSubscriptionFields(meq(expectedApiSubscriptionKey))(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])
    }

    "return 500 error response when subscription fields call fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))

      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
      verifyZeroInteractions(mockPayloadDecorator)
      verifyZeroInteractions(mockMdgDeclarationConnector)
    }

    "return 500 error response when MDG call fails" in new SetUp() {
      when(mockMdgDeclarationConnector.send(any[NodeSeq], any[DateTime], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))

      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId.withNrSubmissionId(nrSubmissionId))
    }

    "return 500 error response when MDG circuit breaker trips" in new SetUp() {
      when(mockMdgDeclarationConnector.send(any[NodeSeq], any[DateTime], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(new UnhealthyServiceException("wco-declaration")))

      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Left(errorResponseServiceUnavailable.XmlResult.withNrSubmissionId(nrSubmissionId))
    }

  "should not have nrs receipt id when call to nrs does not return in time" in new SetUp() {

    when(mockNrsService.send(vpr, headerCarrier)).thenReturn(Future {
      Thread.sleep(1000)
      nrSubmissionId
    })

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Right(None)
    verify(mockMdgDeclarationConnector).send(meq(wrappedValidXML), any[DateTime], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])
  }

  "when NRS disabled should not get a submission id" in new SetUp() {

    when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigDisabled)

    val result: Either[Result, Option[NrSubmissionId]] = send()

    result shouldBe Right(None)
    verify(mockMdgDeclarationConnector).send(meq(wrappedValidXML), any[DateTime], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])
  }

}

