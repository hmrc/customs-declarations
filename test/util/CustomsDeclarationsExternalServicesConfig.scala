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

package util

object CustomsDeclarationsExternalServicesConfig {
  val MdgWcoDecV1ServiceContext = "/mdgWcoDecServiceV1/submitdeclaration"
  val MdgWcoDecV2ServiceContext = "/mdgWcoDecServiceV2/submitdeclaration"
  val MdgWcoDecV3ServiceContext = "/mdgWcoDecServiceV3/submitdeclaration"
  val MdgCancellationDeclarationServiceContext = "/mdgCancelDecService/submit"
  val MdgCancellationDeclarationServiceContextV2 = "/mdgCancelDecService2/submit2"
  val MdgCancellationDeclarationServiceContextV3 = "/mdgCancelDecService3/submit3"
  val MdgStatusDeclarationServiceContextV2 = "/mdgStatusDecService2/submit2"
  val MdgStatusDeclarationServiceContextV3 = "/mdgStatusDecService3/submit3"
  val ApiSubscriptionFieldsContext = "/api-subscription-fields/field"
  val GoogleAnalyticsContext = "/send-google-analytics"
  val GoogleAnalyticsTrackingId = "ga-tracking-id"
  val GoogleAnalyticsClientId = "ga-client-id"
  val GoogleAnalyticsEventValue = "ga-event-value"
  val UpscanInitiateContext = "/upscan/initiate"
  val FileTransmissionContext = "/file/transmission"
  val NrsServiceContext = "/submission"
  val CustomsNotificationAuthHeaderValue = "some-basic-auth"
}
