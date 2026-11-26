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

package uk.gov.hmrc.customs.declaration.services

import play.api.Logger
import uk.gov.hmrc.customs.declaration.connectors.dec64.{MdgConnector, MdgRequestError, MdgRequestFatalError, MdgRequestSuccessful}
import uk.gov.hmrc.customs.declaration.model.transmission.{FailedDeliveryAttempt, TransmissionRequestEnvelope}
import uk.gov.hmrc.customs.declaration.services.queue._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TransmissionRequestProcessingJob @Inject()(
  mdgConnector: MdgConnector,
  clock       : Clock
)(using ExecutionContext) extends QueueJob {

  private val logger = Logger(getClass)

  override def process(
    item         : TransmissionRequestEnvelope,
    nextRetryTime: Instant,
    timeToGiveUp : Instant
  ): Future[ProcessingResult] = {
    given HeaderCarrier = HeaderCarrier()
    val now = clock.instant()

    for
      result <- mdgConnector.requestTransmission(item.request)
    yield
      logIfDeliveryWindowHasExpired(item, now, timeToGiveUp, result.error)

      result match
        case MdgRequestSuccessful =>
          ProcessingSuccessful
        case MdgRequestFatalError(error) =>
          ProcessingFailedDoNotRetry(error)
        case MdgRequestError(error) =>
          if (nextRetryTime.isBefore(timeToGiveUp))
            ProcessingFailed(error)
          else
            ProcessingFailedDoNotRetry(error)
  }

  private def logIfDeliveryWindowHasExpired(
    envelope    : TransmissionRequestEnvelope,
    now         : Instant,
    timeToGiveUp: Instant,
    error       : Option[String]
  ): Unit =
    if (now.isAfter(timeToGiveUp)) {
      val newFailedDeliveryAttempt: Option[FailedDeliveryAttempt] =
        error.map(e => FailedDeliveryAttempt(now, e))

      val updatedDeliveryAttempts: Seq[FailedDeliveryAttempt] = envelope.deliveryAttempts ++ newFailedDeliveryAttempt

      logger.warn(
        s"""Failed to deliver notification within delivery window for file reference: [${envelope.request.file.reference}] before [$timeToGiveUp].
           | Failed delivery attempts were: [${updatedDeliveryAttempts.mkString(",")}].
           | """.stripMargin
      )
    }
}
