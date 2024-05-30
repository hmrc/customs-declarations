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

package uk.gov.hmrc.customs.declaration.services

import cats.implicits._
import uk.gov.hmrc.customs.declaration.config.{ConfigValidatedNelAdaptor, CustomsValidatedNel}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._

import javax.inject.{Inject, Singleton}

@Singleton
class DeclarationsConfigService @Inject()(configValidatedNel: ConfigValidatedNelAdaptor, logger: DeclarationsLogger) {

  private val root = configValidatedNel.root
  private val nrsService = configValidatedNel.service("nrs")
  private val upscanServiceV1 = configValidatedNel.service("upscan-initiate-v1")
  private val upscanServiceV2 = configValidatedNel.service("upscan-initiate-v2")
  private val customsNotificationsService = configValidatedNel.service("customs-notification")
  private val apiSubscriptionFieldsService = configValidatedNel.service("api-subscription-fields")
  private val fileTransmissionService = configValidatedNel.service("file-transmission")
  private val customsDeclarationsMetricsService = configValidatedNel.service("customs-declarations-metrics")

  private val v1ShutteredNel = root.maybeBoolean("shutter.v1")
  private val v2ShutteredNel = root.maybeBoolean("shutter.v2")
  private val v3ShutteredNel = root.maybeBoolean("shutter.v3")

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
  private val nrsUrl = nrsService.serviceUrl

  private val upscanInitiateUrlV1 = upscanServiceV1.serviceUrl
  private val upscanInitiateUrlV2 = upscanServiceV2.serviceUrl
  private val fileUploadUpscanCallbackUrl = root.string("file-upload-upscan-callback.url")
  private val fileGroupSizeMaximum = root.int("fileUpload.fileGroupSize.maximum")
  private val fileTransmissionUrl = fileTransmissionService.serviceUrl
  private val fileTransmissionCallbackUrl =  root.string("file-transmission-callback.url")
  private val upscanInitiateMaximumFileSize = root.int("fileUpload.fileSize.maximum")
  private val ttlInSeconds = root.int("ttlInSeconds")

  private val validatedDeclarationsConfig: CustomsValidatedNel[DeclarationsConfig] = (
    apiSubscriptionFieldsServiceUrlNel, customsNotificationsServiceUrlNel, customsDeclarationsMetricsServiceUrlNel, bearerTokenNel,
    declarationStatusRequestDaysLimit
    ) mapN DeclarationsConfig

  private val validatedDeclarationsShutterConfig: CustomsValidatedNel[DeclarationsShutterConfig] = (
    v1ShutteredNel, v2ShutteredNel, v3ShutteredNel
  ) mapN DeclarationsShutterConfig

  private val validatedDeclarationsCircuitBreakerConfig: CustomsValidatedNel[DeclarationsCircuitBreakerConfig] = (
    numberOfCallsToTriggerStateChangeNel, unavailablePeriodDurationInMillisNel, unstablePeriodDurationInMillisNel
    ) mapN DeclarationsCircuitBreakerConfig

  private val validatedNrsConfig: CustomsValidatedNel[NrsConfig] = (
    nrsEnabled, nrsApiKey, nrsUrl
    ) mapN NrsConfig

  private val validatedFileUploadConfig: CustomsValidatedNel[FileUploadConfig] = (
    upscanInitiateUrlV1, upscanInitiateUrlV2, upscanInitiateMaximumFileSize,
    fileUploadUpscanCallbackUrl, fileGroupSizeMaximum, fileTransmissionCallbackUrl, fileTransmissionUrl, ttlInSeconds
  ) mapN FileUploadConfig

  private val customsConfigHolder =
    (validatedDeclarationsConfig,
      validatedDeclarationsShutterConfig,
      validatedDeclarationsCircuitBreakerConfig,
      validatedNrsConfig,
      validatedFileUploadConfig
      ) mapN CustomsConfigHolder

  private val customsConfigHolderConf =
    customsConfigHolder.fold(
        fe = { nel =>
          // error case exposes nel (a NotEmptyList)
          val errorMsg = nel.toList.mkString("\n", "\n", "")
          logger.errorWithoutRequestContext(errorMsg)
          throw new IllegalStateException(errorMsg)
        },
        fa = identity
      )

  val declarationsConfig: DeclarationsConfig = customsConfigHolderConf.declarationsConfig

  val declarationsShutterConfig: DeclarationsShutterConfig = customsConfigHolderConf.declarationsShutterConfig

  val declarationsCircuitBreakerConfig: DeclarationsCircuitBreakerConfig = customsConfigHolderConf.declarationsCircuitBreakerConfig

  val nrsConfig: NrsConfig = customsConfigHolderConf.validatedNrsConfig

  val fileUploadConfig: FileUploadConfig = customsConfigHolderConf.validatedFileUploadConfig

  private case class CustomsConfigHolder(declarationsConfig: DeclarationsConfig,
                                         declarationsShutterConfig: DeclarationsShutterConfig,
                                         declarationsCircuitBreakerConfig: DeclarationsCircuitBreakerConfig,
                                         validatedNrsConfig: NrsConfig,
                                         validatedFileUploadConfig: FileUploadConfig)
}
