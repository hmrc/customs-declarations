/*
 * Copyright 2026 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.connectors.dec64.{MdgConnector, MdgRequestError, MdgRequestFatalError, MdgRequestResult, MdgRequestSuccessful}
import uk.gov.hmrc.customs.declaration.model.transmission.{FailedDeliveryAttempt, TransmissionRequest, TransmissionRequestEnvelope}
import uk.gov.hmrc.customs.declaration.services.TransmissionRequestProcessingJob
import uk.gov.hmrc.customs.declaration.services.queue.{ProcessingFailed, ProcessingFailedDoNotRetry, ProcessingSuccessful}
import uk.gov.hmrc.http.HeaderCarrier
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.TransmissionRequestTestData.TransmissionRequestOne

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransmissionRequestProcessingJobSpec extends AnyWordSpecLike with Matchers with MockitoSugar {

  private val now = Instant.parse("2026-01-01T10:00:00Z")
  private val clock = Clock.fixed(now, ZoneOffset.UTC)

  private val envelope = TransmissionRequestEnvelope(
    request          = TransmissionRequestOne,
    serviceName      = "customs-declarations",
    csId             = subscriptionFieldsId,
    deliveryAttempts = Seq(FailedDeliveryAttempt(now.minusSeconds(60), "earlier failure"))
  )

  private val inFuture = now.plusSeconds(3600)
  private val inPast   = now.minusSeconds(3600)

  trait SetUp {
    val mockMdgConnector: MdgConnector = mock[MdgConnector]
    val job = new TransmissionRequestProcessingJob(mockMdgConnector, clock)

    def mdgReturns(result: MdgRequestResult): Unit =
      when(mockMdgConnector.requestTransmission(any[TransmissionRequest])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(result))
  }

  "TransmissionRequestProcessingJob" should {

    "return ProcessingSuccessful when MDG accepts the request" in new SetUp {
      mdgReturns(MdgRequestSuccessful)

      await(job.process(envelope, nextRetryTime = now, timeToGiveUp = inFuture)) shouldBe ProcessingSuccessful
      verify(mockMdgConnector).requestTransmission(any[TransmissionRequest])(using any[HeaderCarrier])
    }

    "return ProcessingFailedDoNotRetry when MDG returns a fatal error" in new SetUp {
      mdgReturns(MdgRequestFatalError("bad request"))

      await(job.process(envelope, nextRetryTime = now, timeToGiveUp = inFuture)) shouldBe ProcessingFailedDoNotRetry("bad request")
    }

    "return ProcessingFailed when MDG returns a retryable error and next retry is before the give up time" in new SetUp {
      mdgReturns(MdgRequestError("server error"))

      await(job.process(envelope, nextRetryTime = now.plusSeconds(60), timeToGiveUp = inFuture)) shouldBe ProcessingFailed("server error")
    }

    "return ProcessingFailedDoNotRetry when MDG returns a retryable error and next retry is after the give up time" in new SetUp {
      mdgReturns(MdgRequestError("server error"))

      await(job.process(envelope, nextRetryTime = inFuture.plusSeconds(60), timeToGiveUp = inFuture)) shouldBe ProcessingFailedDoNotRetry("server error")
    }

    "return ProcessingFailedDoNotRetry when the delivery window has already expired" in new SetUp {
      mdgReturns(MdgRequestError("server error"))

      await(job.process(envelope, nextRetryTime = now.plusSeconds(60), timeToGiveUp = inPast)) shouldBe ProcessingFailedDoNotRetry("server error")
    }

    "still return ProcessingSuccessful when delivered after the delivery window has expired" in new SetUp {
      mdgReturns(MdgRequestSuccessful)

      await(job.process(envelope, nextRetryTime = now, timeToGiveUp = inPast)) shouldBe ProcessingSuccessful
    }
  }
}
