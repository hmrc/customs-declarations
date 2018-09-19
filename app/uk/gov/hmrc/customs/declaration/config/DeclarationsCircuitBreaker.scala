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

package uk.gov.hmrc.customs.declaration.config

import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, Upstream4xxResponse}

trait DeclarationsCircuitBreaker extends UsingCircuitBreaker {

  def serviceConfigProvider: ServiceConfigProvider
  def config: DeclarationsConfigService
  def configKey: String

  override protected def circuitBreakerConfig: CircuitBreakerConfig =
    CircuitBreakerConfig(
      serviceName = configKey,
      numberOfCallsToTriggerStateChange = config.declarationsCircuitBreakerConfig.numberOfCallsToTriggerStateChange,
      unavailablePeriodDuration = config.declarationsCircuitBreakerConfig.unavailablePeriodDurationInMillis,
      unstablePeriodDuration = config.declarationsCircuitBreakerConfig.unstablePeriodDurationInMillis
    )

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case _: BadRequestException | _: NotFoundException | _: Upstream4xxResponse => false
    case _ => true
  }
}
