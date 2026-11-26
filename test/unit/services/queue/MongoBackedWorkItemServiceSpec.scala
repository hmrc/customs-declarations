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

package unit.services.queue

import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.*
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.config.ServiceConfiguration
import uk.gov.hmrc.customs.declaration.model.transmission.{FailedDeliveryAttempt, TransmissionRequestEnvelope}
import uk.gov.hmrc.customs.declaration.repo.TransmissionRequestRepo
import uk.gov.hmrc.customs.declaration.services.queue.*
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.TransmissionRequestTestData.TransmissionRequestOne

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

class MongoBackedWorkItemServiceSpec extends AnyWordSpecLike with Matchers with MockitoSugar with Eventually {

  private val now = Instant.parse("2026-01-01T10:00:00Z")
  private val clock = Clock.fixed(now, ZoneOffset.UTC)
  private val receivedAt = now.minusSeconds(600)

  private val envelope = TransmissionRequestEnvelope(
    request     = TransmissionRequestOne,
    serviceName = "customs-declarations",
    csId        = subscriptionFieldsId
  )

  private def workItem(item: TransmissionRequestEnvelope = envelope, failureCount: Int = 0) = WorkItem(
    id           = new ObjectId(),
    receivedAt   = receivedAt,
    updatedAt    = receivedAt,
    availableAt  = receivedAt,
    status       = ProcessingStatus.InProgress,
    failureCount = failureCount,
    item         = item
  )

  trait SetUp {
    val mockRepo: TransmissionRequestRepo = mock[TransmissionRequestRepo]
    val mockJob: QueueJob = mock[QueueJob]
    val mockConfig: ServiceConfiguration = mock[ServiceConfiguration]

    when(mockConfig.initialBackoffAfterFailure).thenReturn(1.minute)
    when(mockConfig.defaultDeliveryWindowDuration).thenReturn(4.hours)

    val service = new MongoBackedWorkItemService(mockRepo, mockJob, mockConfig, clock)

    def outstanding(item: Option[WorkItem[TransmissionRequestEnvelope]]): Unit =
      when(mockRepo.pullOutstanding(ameq(now), ameq(now))).thenReturn(Future.successful(item))

    def jobReturns(result: ProcessingResult): Unit =
      when(mockJob.process(any[TransmissionRequestEnvelope], any[Instant], any[Instant])).thenReturn(Future.successful(result))

    def stubFailureUpdates(): Unit = {
      when(mockRepo.markAs(any[ObjectId], any[ProcessingStatus], any[Option[Instant]])).thenReturn(Future.successful(true))
      when(mockRepo.updateWorkItemBodyDeliveryAttempts(any[ObjectId], any[TransmissionRequestEnvelope])).thenReturn(Future.successful(true))
    }
  }

  "MongoBackedWorkItemService.enqueue" should {
    "push the request onto the repository" in new SetUp {
      when(mockRepo.pushNew(ameq(envelope), ameq(now), any())).thenReturn(Future.successful(workItem()))

      await(service.enqueue(envelope)) shouldBe (())
      verify(mockRepo).pushNew(ameq(envelope), ameq(now), any())
    }
  }

  "MongoBackedWorkItemService.clearQueue" should {
    "delegate to the repository" in new SetUp {
      when(mockRepo.clearRequestQueue()).thenReturn(Future.successful(true))

      await(service.clearQueue()) shouldBe true
      verify(mockRepo).clearRequestQueue()
    }
  }

  "MongoBackedWorkItemService.processOne" should {

    "return false and not call the job when there is nothing outstanding" in new SetUp {
      outstanding(None)

      await(service.processOne()) shouldBe false
      verifyNoInteractions(mockJob)
    }

    "mark the item as succeeded when processing succeeds" in new SetUp {
      val item = workItem()
      outstanding(Some(item))
      jobReturns(ProcessingSuccessful)
      when(mockRepo.complete(any[ObjectId], any())).thenReturn(Future.successful(true))

      await(service.processOne()) shouldBe true

      verify(mockJob).process(ameq(envelope), ameq(now.plusMillis(1.minute.toMillis)), ameq(receivedAt.plusMillis(4.hours.toMillis)))
      eventually {
        verify(mockRepo).complete(ameq(item.id), ameq(ProcessingStatus.Succeeded))
        ()
      }
    }

    "use the request's own delivery window and exponential backoff when calculating times" in new SetUp {
      val customEnvelope = envelope.copy(request = TransmissionRequestOne.copy(deliveryWindowDuration = Some(30.minutes)))
      val item = workItem(customEnvelope, failureCount = 2)
      outstanding(Some(item))
      jobReturns(ProcessingSuccessful)
      when(mockRepo.complete(any[ObjectId], any())).thenReturn(Future.successful(true))

      await(service.processOne()) shouldBe true

      verify(mockJob).process(ameq(customEnvelope), ameq(now.plusMillis(4.minutes.toMillis)), ameq(receivedAt.plusMillis(30.minutes.toMillis)))
    }

    "mark the item as failed with a retry time and record the delivery attempt when processing fails" in new SetUp {
      val item = workItem()
      outstanding(Some(item))
      jobReturns(ProcessingFailed("server error"))
      stubFailureUpdates()

      await(service.processOne()) shouldBe true

      eventually {
        verify(mockRepo).markAs(ameq(item.id), ameq(ProcessingStatus.Failed), ameq(Some(now.plusMillis(1.minute.toMillis))))
        verify(mockRepo).updateWorkItemBodyDeliveryAttempts(
          ameq(item.id),
          ameq(envelope.withFailedDeliveryAttempt(FailedDeliveryAttempt(now, "server error")))
        )
        ()
      }
    }

    "mark the item as permanently failed and record the delivery attempt when processing fails without retry" in new SetUp {
      val item = workItem()
      outstanding(Some(item))
      jobReturns(ProcessingFailedDoNotRetry("bad request"))
      stubFailureUpdates()

      await(service.processOne()) shouldBe true

      eventually {
        verify(mockRepo).markAs(ameq(item.id), ameq(ProcessingStatus.PermanentlyFailed), ameq(None))
        verify(mockRepo).updateWorkItemBodyDeliveryAttempts(
          ameq(item.id),
          ameq(envelope.withFailedDeliveryAttempt(FailedDeliveryAttempt(now, "bad request")))
        )
        ()
      }
    }

    "fail when the job fails" in new SetUp {
      outstanding(Some(workItem()))
      when(mockJob.process(any[TransmissionRequestEnvelope], any[Instant], any[Instant]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      intercept[RuntimeException](await(service.processOne())).getMessage shouldBe "boom"
    }
  }
}
