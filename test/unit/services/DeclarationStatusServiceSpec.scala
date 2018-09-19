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
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedStatusRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.{correlationId, _}

import scala.concurrent.Future

class DeclarationStatusServiceSpec extends UnitSpec with MockitoSugar {
  private val dateTime = new DateTime()
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

  trait SetUp {
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected lazy val mockDeclarationStatusConnector: DeclarationStatusConnector = mock[DeclarationStatusConnector]
    protected val mockPayloadDecorator: MdgPayloadDecorator = mock[MdgPayloadDecorator]
    protected val mockDateTimeProvider: DateTimeService = mock[DateTimeService]
    protected val mockHttpResponse: HttpResponse = mock[HttpResponse]
    protected val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    protected val mrn = Mrn("theMrn")

    protected lazy val service: DeclarationStatusService = new DeclarationStatusService(mockLogger, mockDeclarationStatusConnector, mockDateTimeProvider, stubUniqueIdsService)

    protected def send(vpr: AuthorisedStatusRequest[AnyContentAsXml] = TestAuthorisedStatusRequest, hc: HeaderCarrier = headerCarrier): Either[Result, HttpResponse] = {
      await(service.send(mrn) (vpr, hc))
    }

    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockDeclarationStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], meq[UUID](dmirId.uuid).asInstanceOf[DeclarationManagementInformationRequestId], any[ApiVersion], meq[String](mrn.value).asInstanceOf[Mrn])(any[AuthorisedStatusRequest[_]])).thenReturn(Future.successful(mockHttpResponse))
  }
  "BusinessService" should {

    "send transformed xml to connector" in new SetUp() {

        val result: Either[Result, HttpResponse] = send()

        result shouldBe Right(mockHttpResponse)
        verify(mockDeclarationStatusConnector).send(dateTime, correlationId, dmirId, VersionTwo, mrn)(TestAuthorisedStatusRequest)
      }
    }

    "return 500 error response when MDG call fails" in new SetUp() {
      when(mockDeclarationStatusConnector.send(any[DateTime], meq[UUID](correlationId.uuid).asInstanceOf[CorrelationId], meq[UUID](dmirId.uuid).asInstanceOf[DeclarationManagementInformationRequestId], any[ApiVersion], meq[String](mrn.value).asInstanceOf[Mrn])(any[AuthorisedStatusRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))
      val result: Either[Result, HttpResponse] = send()

      result shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
}

