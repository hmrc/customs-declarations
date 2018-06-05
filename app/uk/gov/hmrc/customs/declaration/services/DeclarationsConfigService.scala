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

package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}
import scalaz.ValidationNel
import scalaz.syntax.apply._
import scalaz.syntax.traverse._
import uk.gov.hmrc.customs.api.common.config.ConfigValidationNelAdaptor
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{DeclarationsCircuitBreakerConfig, DeclarationsConfig}

@Singleton
class DeclarationsConfigService @Inject()(configValidationNel: ConfigValidationNelAdaptor, logger: DeclarationsLogger) {

  private val root = configValidationNel.root
  private val customsNotificationsService = configValidationNel.service("customs-notification")
  private val apiSubscriptionFieldsService = configValidationNel.service("api-subscription-fields")
  private val googleAnalyticsService = configValidationNel.service("google-analytics")

  private val numberOfCallsToTriggerStateChangeNel = root.int("circuitBreaker.numberOfCallsToTriggerStateChange")
  private val unavailablePeriodDurationInMillisNel = root.int("circuitBreaker.unavailablePeriodDurationInMillis")
  private val unstablePeriodDurationInMillisNel = root.int("circuitBreaker.unstablePeriodDurationInMillis")
  private val bearerTokenNel = customsNotificationsService.string("bearer-token")
  private val customsNotificationsServiceUrlNel = customsNotificationsService.serviceUrl
  private val apiSubscriptionFieldsServiceUrlNel = apiSubscriptionFieldsService.serviceUrl
  private val gaServiceUrlNel: ValidationNel[String, String] = googleAnalyticsService.serviceUrl

  private val validatedDeclarationsConfig: ValidationNel[String, DeclarationsConfig] = (
    apiSubscriptionFieldsServiceUrlNel |@| customsNotificationsServiceUrlNel |@| gaServiceUrlNel |@| bearerTokenNel
    ) (DeclarationsConfig.apply)

  private val validatedDeclarationsCircuitBreakerConfig: ValidationNel[String, DeclarationsCircuitBreakerConfig] = (
    numberOfCallsToTriggerStateChangeNel |@| unavailablePeriodDurationInMillisNel |@| unstablePeriodDurationInMillisNel
    ) (DeclarationsCircuitBreakerConfig.apply)

  private val customsConfigHolder =
    (validatedDeclarationsConfig |@|
      validatedDeclarationsCircuitBreakerConfig) (CustomsConfigHolder.apply) fold(
      fail = { nel =>
        // error case exposes nel (a NotEmptyList)
        val errorMsg = nel.toList.mkString("\n", "\n", "")
        logger.errorWithoutRequestContext(errorMsg)
        throw new IllegalStateException(errorMsg)
      },
      succ = identity
    )

  val declarationsConfig: DeclarationsConfig = customsConfigHolder.declarationsConfig

  val declarationsCircuitBreakerConfig: DeclarationsCircuitBreakerConfig = customsConfigHolder.declarationsCircuitBreakerConfig

  private case class CustomsConfigHolder(declarationsConfig: DeclarationsConfig,
                                         declarationsCircuitBreakerConfig: DeclarationsCircuitBreakerConfig)
}
