/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.connectors

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreaker
import uk.gov.hmrc.customs.declaration.logging.CdsLogger

import java.util.concurrent.TimeUnit.MILLISECONDS
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait CircuitBreakerConnector {
  protected val configKey: String
  protected val numberOfCallsToTriggerStateChange: Int
  protected val unstablePeriodDurationInMillis: Int
  protected val unavailablePeriodDurationInMillis: Int

  protected val cdsLogger: CdsLogger
  protected val actorSystem: ActorSystem
  implicit val ec: ExecutionContext

  protected def breakOnException(t: Throwable): Boolean = true

  final def withCircuitBreaker[T](body: => Future[T]): Future[T] =
    breaker.withCircuitBreaker(body, defineFailure)

  cdsLogger.info(s"Circuit Breaker [$configKey] instance created with config: numberOfCallsToTriggerStateChange: $numberOfCallsToTriggerStateChange, unstablePeriodDurationInMillis: $unstablePeriodDurationInMillis, unavailablePeriodDurationInMillis: $unavailablePeriodDurationInMillis")
  private lazy val breaker = new CircuitBreaker(
    scheduler = actorSystem.scheduler,
    maxFailures = numberOfCallsToTriggerStateChange,
    callTimeout = Duration(unstablePeriodDurationInMillis, MILLISECONDS),
    resetTimeout = Duration(unavailablePeriodDurationInMillis, MILLISECONDS))
      .onOpen(notifyOnStateChange("Open"))
      .onClose(notifyOnStateChange("Close"))
      .onHalfOpen(notifyOnStateChange("HalfOpen"))

  private def notifyOnStateChange(newState: String): Unit =
    cdsLogger.warn(s"circuitbreaker: Service [$configKey] is in state [${newState}]")

  private def defineFailure(t: Try[?]): Boolean =
    t.isFailure && breakOnException(t.failed.get)
}
