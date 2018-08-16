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

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{verify, verifyZeroInteractions, when}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, MdgWcoDeclarationConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, _}
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData._
import util.TestData._

import scala.concurrent.Future
import scala.xml.NodeSeq

class StandardDeclarationSubmissionServiceSpec extends UnitSpec with MockitoSugar {

  private val dateTime = new DateTime()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest
  private val wrappedValidXML = <wrapped></wrapped>

  trait SetUp {
    protected val mockLogger = mock[DeclarationsLogger]
    protected val mockMdgDeclarationConnector = mock[MdgWcoDeclarationConnector]
    protected val mockApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockPayloadDecorator = mock[MdgPayloadDecorator]
    protected val mockDateTimeProvider = mock[DateTimeService]
    protected val mockHttpResponse = mock[HttpResponse]
    protected val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    protected val mockNrsService: NrsService = mock[NrsService]

    protected lazy val service = new StandardDeclarationSubmissionService(mockLogger,
      mockMdgDeclarationConnector,
      mockApiSubscriptionFieldsConnector,
      mockPayloadDecorator,
      mockDateTimeProvider,
      stubUniqueIdsService,
      mockNrsService,
      mockDeclarationsConfigService)

    protected def send(vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): Either[Result, Option[NrSubmissionId]] = {
      await(service.send(vpr, hc))
    }

    when(mockPayloadDecorator.wrap(meq(TestXmlPayload), meq[String](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId], any[DateTime])(any[ValidatedPayloadRequest[_]])).thenReturn(wrappedValidXML)
    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockMdgDeclarationConnector.send(any[NodeSeq], meq(dateTime), any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.successful(mockHttpResponse))
    when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
    when(mockNrsService.send(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(new NrsResponsePayload(nrSubmissionId)))
  }
  "StandardDeclarationSubmissionService" should {

    "send to connector when nrs enabled" in new SetUp() {
      when(mockDeclarationsConfigService.nrsConfig).thenReturn(NrsConfig(true, "nrs-api-key"))

      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Right(Some(nrSubmissionId))

      verify(mockNrsService).send(meq(vpr), any[HeaderCarrier])
    }

      "not send to connector when nrs disabled" in new SetUp() {
        when(mockDeclarationsConfigService.nrsConfig).thenReturn(NrsConfig(false, "nrs-api-key"))

        val result: Either[Result, Option[NrSubmissionId]] = send()
        result shouldBe Right(None)

        verifyZeroInteractions(mockNrsService)
      }

    "should still contain nrs submission id even if call to downstream fails" in new SetUp() {
      when(mockDeclarationsConfigService.nrsConfig).thenReturn(NrsConfig(true, "nrs-api-key"))
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(ApiSubscriptionFieldsResponse(subscriptionFieldsIdUuid)))
      when(mockMdgDeclarationConnector.send(any[NodeSeq], any[DateTime], any[UUID], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))
      val result: Either[Result, Option[NrSubmissionId]] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId.withNrSubmissionId(nrSubmissionId))
    }
  }
}

