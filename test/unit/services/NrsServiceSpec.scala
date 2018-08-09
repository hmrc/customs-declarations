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

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.mvc._
import uk.gov.hmrc.customs.declaration.connectors.NrsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, _}
import uk.gov.hmrc.customs.declaration.services.{DateTimeService, NrsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData
import util.TestData._

import scala.concurrent.Future

class NrsServiceSpec extends UnitSpec with MockitoSugar {
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

  trait SetUp {
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]
    protected val mockNrsConnector: NrsConnector = mock[NrsConnector]

    protected lazy val service: NrsService = new NrsService(mockLogger, mockNrsConnector, mockDateTimeService)

    protected val nrsResponsePayload = new NrsResponsePayload(TestData.nrSubmissionId)
    protected val dateTime = new DateTime()

    protected def send(vupr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): Future[NrsResponsePayload] = {
      when(mockDateTimeService.nowUtc()).thenReturn(TestData.nrsTimeStamp)
      await(service.send(vupr, hc))
    }
  }

  "NrsService" should {
    "send payload to connector" in new SetUp() {

      when(mockNrsConnector.send(any[NrsPayload], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.successful(nrsResponsePayload))
      val result = send()
      await(result) shouldBe nrsResponsePayload
      verify(mockNrsConnector).send(meq(TestData.nrsPayload), any[ApiVersion])(any[ValidatedPayloadRequest[_]])
    }
  }

  "return failed future when nrs service call fails" in new SetUp() {

    when(mockNrsConnector.send(any[NrsPayload], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(new Exception()))

    val result = send()
    ScalaFutures.whenReady(result.failed) { ex =>
      ex shouldBe a [Exception]
    }
  }
}

