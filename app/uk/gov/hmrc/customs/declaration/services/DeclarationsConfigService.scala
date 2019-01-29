/*
 * Copyright 2019 HM Revenue & Customs
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

import cats.implicits._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.api.common.config.{ConfigValidatedNelAdaptor, CustomsValidatedNel}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._

@Singleton
class DeclarationsConfigService @Inject()(configValidatedNel: ConfigValidatedNelAdaptor, logger: DeclarationsLogger) {

  private val root = configValidatedNel.root
  private val nrsService = configValidatedNel.service("nrs")
  private val upscanService = configValidatedNel.service("upscan-initiate")
  private val customsNotificationsService = configValidatedNel.service("customs-notification")
  private val apiSubscriptionFieldsService = configValidatedNel.service("api-subscription-fields")
  private val fileTransmissionService = configValidatedNel.service("file-transmission")
  private val customsDeclarationsMetricsService = configValidatedNel.service("customs-declarations-metrics")

  private val numberOfCallsToTriggerStateChangeNel = root.int("circuitBreaker.numberOfCallsToTriggerStateChange")
  private val unavailablePeriodDurationInMillisNel = root.int("circuitBreaker.unavailablePeriodDurationInMillis")
  private val unstablePeriodDurationInMillisNel = root.int("circuitBreaker.unstablePeriodDurationInMillis")

  private val declarationStatusRequestDaysLimit = root.int("declarationStatus.requestDaysLimit")

  private val bearerTokenNel = customsNotificationsService.string("bearer-token")
  private val customsNotificationsServiceUrlNel = customsNotificationsService.serviceUrl
  private val apiSubscriptionFieldsServiceUrlNel = apiSubscriptionFieldsService.serviceUrl
  private val customsDeclarationsMetricsServiceUrlNel = customsDeclarationsMetricsService.serviceUrl

  private val nrsEnabled = root.boolean("nrs.enabled")
  private val nrsApiKey = root.string("nrs.apikey")
  private val nrsWaitTimeMillis = root.int("nrs.waittime.millis")
  private val nrsUrl = nrsService.serviceUrl

  private val upscanInitiateUrl = upscanService.serviceUrl
  private val upscanCallbackUrl = root.string("upscan-callback.url")
  private val fileUploadUpscanCallbackUrl = root.string("file-upload-upscan-callback.url")
  private val fileGroupSizeMaximum = root.int("fileUpload.fileGroupSize.maximum")
  private val fileTransmissionUrl = fileTransmissionService.serviceUrl
  private val fileTransmissionCallbackUrl =  root.string("file-transmission-callback.url")

  private val validatedDeclarationsConfig: CustomsValidatedNel[DeclarationsConfig] = (
    apiSubscriptionFieldsServiceUrlNel, customsNotificationsServiceUrlNel, customsDeclarationsMetricsServiceUrlNel, bearerTokenNel, declarationStatusRequestDaysLimit
    ) mapN DeclarationsConfig

  private val validatedDeclarationsCircuitBreakerConfig: CustomsValidatedNel[DeclarationsCircuitBreakerConfig] = (
    numberOfCallsToTriggerStateChangeNel, unavailablePeriodDurationInMillisNel, unstablePeriodDurationInMillisNel
    ) mapN DeclarationsCircuitBreakerConfig

  private val validatedNrsConfig: CustomsValidatedNel[NrsConfig] = (
    nrsEnabled, nrsApiKey, nrsUrl
    ) mapN NrsConfig

  private val validatedFileUploadConfig: CustomsValidatedNel[FileUploadConfig] = (
    upscanInitiateUrl, upscanCallbackUrl, fileUploadUpscanCallbackUrl, fileGroupSizeMaximum, fileTransmissionCallbackUrl, fileTransmissionUrl
  ) mapN FileUploadConfig

  private val customsConfigHolder =
    (validatedDeclarationsConfig,
      validatedDeclarationsCircuitBreakerConfig,
      validatedNrsConfig,
      validatedFileUploadConfig
      ) mapN CustomsConfigHolder fold(
        fe = { nel =>
          // error case exposes nel (a NotEmptyList)
          val errorMsg = nel.toList.mkString("\n", "\n", "")
          logger.errorWithoutRequestContext(errorMsg)
          throw new IllegalStateException(errorMsg)
        },
        fa = identity
      )

  val declarationsConfig: DeclarationsConfig = customsConfigHolder.declarationsConfig

  val declarationsCircuitBreakerConfig: DeclarationsCircuitBreakerConfig = customsConfigHolder.declarationsCircuitBreakerConfig

  val nrsConfig: NrsConfig = customsConfigHolder.validatedNrsConfig

  val fileUploadConfig: FileUploadConfig = customsConfigHolder.validatedFileUploadConfig

  private case class CustomsConfigHolder(declarationsConfig: DeclarationsConfig,
                                         declarationsCircuitBreakerConfig: DeclarationsCircuitBreakerConfig,
                                         validatedNrsConfig: NrsConfig,
                                         validatedFileUploadConfig: FileUploadConfig)
}
