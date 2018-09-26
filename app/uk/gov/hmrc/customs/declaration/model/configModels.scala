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

package uk.gov.hmrc.customs.declaration.model

case class DeclarationsConfig(apiSubscriptionFieldsBaseUrl: String,
                              customsNotificationBaseBaseUrl: String,
                              customsNotificationBearerToken: String,
                              declarationStatusRequestDaysLimit: Int)

case class NrsConfig(nrsEnabled: Boolean, nrsApiKey: String, nrsWaitTimeMillis: Int)

case class DeclarationsCircuitBreakerConfig(numberOfCallsToTriggerStateChange: Int,
                                            unavailablePeriodDurationInMillis: Int,
                                            unstablePeriodDurationInMillis: Int)

case class GoogleAnalyticsConfig(enabled: Boolean,
                                 url: String,
                                 trackingId: String,
                                 clientId: String,
                                 eventValue: String)

case class BatchFileUploadConfig(upscanCallbackUrl: String,
                                 fileGroupSizeMaximum: Int)
