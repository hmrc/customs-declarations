/*
 * Copyright 2021 HM Revenue & Customs
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
import org.scalatest.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, DeclarationSubmissionConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, _}
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.CustomsDeclarationsMetricsTestData
import util.TestData._

import scala.concurrent.Future
import scala.xml.NodeSeq

class StandardDeclarationSubmissionServiceSpec extends AnyWordSpecLike with MockitoSugar with Matchers{

  private val dateTime = new DateTime()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest
  private implicit val ec = Helpers.stubControllerComponents().executionContext
  private val wrappedValidXML = <wrapped></wrapped>

  trait SetUp {
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockMdgDeclarationConnector: DeclarationSubmissionConnector = mock[DeclarationSubmissionConnector]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockPayloadDecorator: MdgPayloadDecorator = mock[MdgPayloadDecorator]
    protected val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
    protected val mockHttpResponse: HttpResponse = mock[HttpResponse]
    protected val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    protected val mockNrsService: NrsService = mock[NrsService]

    protected lazy val service = new StandardDeclarationSubmissionService(mockLogger,
      mockApiSubscriptionFieldsConnector,
      mockMdgDeclarationConnector,
      mockPayloadDecorator,
      mockDateTimeProvider,
      stubUniqueIdsService,
      mockNrsService,
      mockDeclarationsConfigService,
      ActorSystem("StandardDeclarationSubmissionServiceSpec")
    )

    protected def send(vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): Either[Result, Option[NrSubmissionId]] = {
      await(service.send(vpr, hc))
    }

    when(mockPayloadDecorator.wrap(meq(TestXmlPayload), meq[ApiSubscriptionFieldsResponse](apiSubscriptionFieldsResponse), any[DateTime])(any[ValidatedPayloadRequest[_]])).thenReturn(wrappedValidXML)
    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockDateTimeProvider.zonedDateTimeUtc).thenReturn(CustomsDeclarationsMetricsTestData.EventStart, CustomsDeclarationsMetricsTestData.EventEnd)
    when(mockMdgDeclarationConnector.send(any[NodeSeq], meq(dateTime), any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.successful(mockHttpResponse))
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
    when(mockNrsService.send(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(nrSubmissionId))
  }
  "StandardDeclarationSubmissionService" should {

    "send to connector when nrs enabled" in new SetUp() {
      when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)

      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Right(Some(nrSubmissionId))

      verify(mockNrsService).send(meq(vpr), any[HeaderCarrier])
    }

      "not send to connector when nrs disabled" in new SetUp() {
        when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigDisabled)

        val result: Either[Result, Option[NrSubmissionId]] = send()
        result shouldBe Right(None)

        verifyZeroInteractions(mockNrsService)
      }

    "should still contain nrs submission id even if call to downstream fails" in new SetUp() {
      when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockMdgDeclarationConnector.send(any[NodeSeq], any[DateTime], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))
      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId.withNrSubmissionId(nrSubmissionId))
    }
  }
}

