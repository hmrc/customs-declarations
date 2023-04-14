/*
 * Copyright 2023 HM Revenue & Customs
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

package component

import org.scalatest.Suite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import util.{CustomsDeclarationsExternalServicesConfig, ExternalServicesConfig}

trait FakePlayApplication {
  this: Suite =>

  val mockHost: String = "localhost"
  val mockPort: Int = 1111
  val mockUrl = s"http://$mockHost:$mockPort"


  protected val configMap: Map[String, Any] = Map(
    "xml.max-errors" -> 2,
    "metrics.jvm" -> false,
    "microservice.services.auth.host" -> ExternalServicesConfig.Host,
    "microservice.services.auth.port" -> ExternalServicesConfig.Port,
    "microservice.services.wco-declaration.host" -> ExternalServicesConfig.Host,
    "microservice.services.wco-declaration.port" -> ExternalServicesConfig.Port,
    "microservice.services.wco-declaration.context" -> CustomsDeclarationsExternalServicesConfig.MdgWcoDecV1ServiceContext,
    "microservice.services.wco-declaration.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.v2.wco-declaration.host" -> ExternalServicesConfig.Host,
    "microservice.services.v2.wco-declaration.port" -> ExternalServicesConfig.Port,
    "microservice.services.v2.wco-declaration.context" -> CustomsDeclarationsExternalServicesConfig.MdgWcoDecV2ServiceContext,
    "microservice.services.v2.wco-declaration.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.v3.wco-declaration.host" -> ExternalServicesConfig.Host,
    "microservice.services.v3.wco-declaration.port" -> ExternalServicesConfig.Port,
    "microservice.services.v3.wco-declaration.context" -> CustomsDeclarationsExternalServicesConfig.MdgWcoDecV3ServiceContext,
    "microservice.services.v3.wco-declaration.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.declaration-cancellation.host" -> ExternalServicesConfig.Host,
    "microservice.services.declaration-cancellation.port" -> ExternalServicesConfig.Port,
    "microservice.services.declaration-cancellation.context" -> CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContext,
    "microservice.services.declaration-cancellation.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.v2.declaration-cancellation.host" -> ExternalServicesConfig.Host,
    "microservice.services.v2.declaration-cancellation.port" -> ExternalServicesConfig.Port,
    "microservice.services.v2.declaration-cancellation.context" -> CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContextV2,
    "microservice.services.v2.declaration-cancellation.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.v3.declaration-cancellation.host" -> ExternalServicesConfig.Host,
    "microservice.services.v3.declaration-cancellation.port" -> ExternalServicesConfig.Port,
    "microservice.services.v3.declaration-cancellation.context" -> CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContextV3,
    "microservice.services.v3.declaration-cancellation.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.v2.declaration-status.host" -> ExternalServicesConfig.Host,
    "microservice.services.v2.declaration-status.port" -> ExternalServicesConfig.Port,
    "microservice.services.v2.declaration-status.context" -> CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContextV2,
    "microservice.services.v2.declaration-status.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.v3.declaration-status.host" -> ExternalServicesConfig.Host,
    "microservice.services.v3.declaration-status.port" -> ExternalServicesConfig.Port,
    "microservice.services.v3.declaration-status.context" -> CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContextV3,
    "microservice.services.v3.declaration-status.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.nrs.host" -> ExternalServicesConfig.Host,
    "microservice.services.nrs.port" -> ExternalServicesConfig.Port,
    "microservice.services.nrs.context" -> CustomsDeclarationsExternalServicesConfig.NrsServiceContext,
    "microservice.services.api-subscription-fields.host" -> ExternalServicesConfig.Host,
    "microservice.services.api-subscription-fields.port" -> ExternalServicesConfig.Port,
    "microservice.services.api-subscription-fields.context" -> CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext,
    "upscan-initiate.url" -> s"http://${ExternalServicesConfig.Host}:${ExternalServicesConfig.Port}${CustomsDeclarationsExternalServicesConfig.UpscanInitiateContextV2}",
    "microservice.services.upscan-initiate-v1.host" -> ExternalServicesConfig.Host,
    "microservice.services.upscan-initiate-v1.port" -> ExternalServicesConfig.Port,
    "microservice.services.upscan-initiate-v1.context" -> CustomsDeclarationsExternalServicesConfig.UpscanInitiateContextV2,
    "microservice.services.upscan-initiate-v1.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.upscan-initiate-v2.host" -> ExternalServicesConfig.Host,
    "microservice.services.upscan-initiate-v2.port" -> ExternalServicesConfig.Port,
    "microservice.services.upscan-initiate-v2.context" -> CustomsDeclarationsExternalServicesConfig.UpscanInitiateContextV2,
    "microservice.services.upscan-initiate-v2.bearer-token" -> ExternalServicesConfig.AuthToken,
    "auditing.enabled" -> false,
    "auditing.consumer.baseUri.host" -> ExternalServicesConfig.Host,
    "auditing.consumer.baseUri.port" -> ExternalServicesConfig.Port,
    "microservice.services.customs-notification.host" -> ExternalServicesConfig.Host,
    "microservice.services.customs-notification.port" -> ExternalServicesConfig.Port,
    "microservice.services.customs-notification.context" -> "/customs-notification/notify",
    "microservice.services.customs-notification.bearer-token" -> CustomsDeclarationsExternalServicesConfig.CustomsNotificationAuthHeaderValue,
    "microservice.services.file-transmission.host" -> ExternalServicesConfig.Host,
    "microservice.services.file-transmission.port" -> ExternalServicesConfig.Port,
    "microservice.services.file-transmission.context" -> CustomsDeclarationsExternalServicesConfig.FileTransmissionContext,
    "microservice.services.customs-declarations-metrics.host" -> ExternalServicesConfig.Host,
    "microservice.services.customs-declarations-metrics.port" -> ExternalServicesConfig.Port,
    "microservice.services.customs-declarations-metrics.context" -> CustomsDeclarationsExternalServicesConfig.CustomsDeclarationsMetricsContext
  )


  lazy val fakeApplication: Application =
    new GuiceApplicationBuilder()
      .configure(
        configMap
      ).build()
}
