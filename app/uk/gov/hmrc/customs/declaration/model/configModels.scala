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

package uk.gov.hmrc.customs.declaration.model

case class DeclarationsConfig(apiSubscriptionFieldsBaseUrl: String,
                              customsNotificationBaseBaseUrl: String,
                              customsDeclarationsMetricsBaseBaseUrl: String,
                              customsNotificationBearerToken: String,
                              declarationStatusRequestDaysLimit: Int)

case class NrsConfig(nrsEnabled: Boolean, nrsApiKey: String,  nrsUrl: String)

case class DeclarationsShutterConfig(v1Shuttered: Option[Boolean],
                                     v2Shuttered: Option[Boolean],
                                     v3Shuttered: Option[Boolean])

case class DeclarationsCircuitBreakerConfig(numberOfCallsToTriggerStateChange: Int,
                                            unavailablePeriodDurationInMillis: Int,
                                            unstablePeriodDurationInMillis: Int)

case class FileUploadConfig(upscanInitiateV1Url: String,
                            upscanInitiateV2Url: String,
                            upscanInitiateMaximumFileSize: Int,
                            fileUploadCallbackUrl: String,
                            fileGroupSizeMaximum: Int,
                            fileTransmissionCallbackUrl: String,
                            fileTransmissionBaseUrl: String,
                            ttlInSeconds: Int)
