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

package uk.gov.hmrc.customs.declaration.config

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import uk.gov.hmrc.customs.api.common.connectors.CircuitBreakerConnector
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId

trait DeclarationCircuitBreaker extends CircuitBreakerConnector {

  def logger: DeclarationsLogger

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case e: Non2xxResponseException => e.responseCode match {
      case 400 => false //BadRequest
      case 404 => false //NotFound
      case _ => true
    }
    case _ => true
  }

  protected def logCallDuration(startTime: LocalDateTime)(implicit r: HasConversationId): Unit ={
    val callDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now)
    logger.info(s"Outbound call duration was ${callDuration} ms")
  }
}
