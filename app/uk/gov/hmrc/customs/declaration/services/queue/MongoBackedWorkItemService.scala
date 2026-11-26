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

package uk.gov.hmrc.customs.declaration.services.queue

import cats.data.OptionT
import cats.implicits._
import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.mongo.workitem.{WorkItem, ResultStatus, ProcessingStatus}
import uk.gov.hmrc.customs.declaration.config.ServiceConfiguration
import uk.gov.hmrc.customs.declaration.model.transmission.{FailedDeliveryAttempt, TransmissionRequestEnvelope}
import uk.gov.hmrc.customs.declaration.repo.TransmissionRequestRepo

sealed trait ProcessingResult
case object ProcessingSuccessful extends ProcessingResult
case class ProcessingFailed(error: String) extends ProcessingResult
case class ProcessingFailedDoNotRetry(error: String) extends ProcessingResult

trait QueueJob {
  def process(
    item         : TransmissionRequestEnvelope,
    nextRetryTime: Instant,
    timeToGiveUp : Instant
  ): Future[ProcessingResult]
}

trait WorkItemService {
  def enqueue(request: TransmissionRequestEnvelope): Future[Unit]

  def processOne(): Future[Boolean]

  def clearQueue(): Future[Boolean]
}

class MongoBackedWorkItemService @Inject()(
  repository   : TransmissionRequestRepo,
  queueJob     : QueueJob,
  configuration: ServiceConfiguration,
  clock        : Clock
)(using ExecutionContext) extends WorkItemService {

  def enqueue(request: TransmissionRequestEnvelope): Future[Unit] =
    repository.pushNew(request, now()).map(_ => ())

  def processOne(): Future[Boolean] = {

    val failedBefore    = now()
    val availableBefore = now()

    val result: OptionT[Future, Unit] = for {
      firstOutstandingItem <- OptionT(repository.pullOutstanding(failedBefore, availableBefore))
      _                    <- OptionT.liftF(processWorkItem(firstOutstandingItem))
    } yield ()

    result.value.map(_.isDefined)
  }

  override def clearQueue(): Future[Boolean] =
    repository.clearRequestQueue()

  private def processWorkItem(
    workItem   : WorkItem[TransmissionRequestEnvelope],
    processedAt: Instant = Instant.now(clock)
  ): Future[Unit] = {

    def updateStatusForFailedDeliveryAttempt(
      status     : ResultStatus,
      availableAt: Option[Instant],
      error      : String
    ): Future[Boolean] = {
      val updatedEnvelope = workItem.item.withFailedDeliveryAttempt(FailedDeliveryAttempt(processedAt, error))

      for
        _       <- repository.markAs(workItem.id, status, availableAt)
        updated <- repository.updateWorkItemBodyDeliveryAttempts(workItem.id, updatedEnvelope)
      yield updated
    }

    val request       = workItem.item
    val nextRetryTime = nextAvailabilityTime(workItem)

    queueJob
      .process(request, nextRetryTime, timeToGiveUp(workItem))
      .flatMap {
        case ProcessingSuccessful =>
          repository.complete(workItem.id, ProcessingStatus.Succeeded)
        case ProcessingFailed(error) =>
          updateStatusForFailedDeliveryAttempt(ProcessingStatus.Failed, Some(nextRetryTime), error)
        case ProcessingFailedDoNotRetry(error) =>
          updateStatusForFailedDeliveryAttempt(ProcessingStatus.PermanentlyFailed, None, error)
      }
      .map(_ => ())
  }

  private def now(): Instant =
    Instant.now(clock)

  private def nextAvailabilityTime[T](workItem: WorkItem[T]): Instant = {
    val instantNow = now()
    val multiplier = Math.pow(2, workItem.failureCount).toInt
    val delay      = configuration.initialBackoffAfterFailure * multiplier
    instantNow.plusMillis(delay.toMillis)
  }

  private def timeToGiveUp(workItem: WorkItem[TransmissionRequestEnvelope]): Instant = {
    val deliveryWindowDuration =
      workItem.item.request.deliveryWindowDuration
        .getOrElse(configuration.defaultDeliveryWindowDuration)

    workItem.receivedAt.plusMillis(deliveryWindowDuration.toMillis)
  }
}
