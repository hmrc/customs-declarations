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

package component

import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import util.{CustomsDeclarationsExternalServicesConfig, ExternalServicesConfig}

import scala.util.control.NonFatal
import scala.xml.{Node, Utility, XML}

trait ComponentTestSpec extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite
  with BeforeAndAfterAll with BeforeAndAfterEach with Eventually {

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(Map(
    "xml.max-errors" -> 2,
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
    "microservice.services.nrs-service.host" -> ExternalServicesConfig.Host,
    "microservice.services.nrs-service.port" -> ExternalServicesConfig.Port,
    "microservice.services.nrs-service.context" -> CustomsDeclarationsExternalServicesConfig.NrsServiceContext,
    "microservice.services.v2.nrs-service.host" -> ExternalServicesConfig.Host,
    "microservice.services.v2.nrs-service.port" -> ExternalServicesConfig.Port,
    "microservice.services.v2.nrs-service.context" -> CustomsDeclarationsExternalServicesConfig.NrsServiceContext,
    "microservice.services.v3.nrs-service.host" -> ExternalServicesConfig.Host,
    "microservice.services.v3.nrs-service.port" -> ExternalServicesConfig.Port,
    "microservice.services.v3.nrs-service.context" -> CustomsDeclarationsExternalServicesConfig.NrsServiceContext,
    "microservice.services.api-subscription-fields.host" -> ExternalServicesConfig.Host,
    "microservice.services.api-subscription-fields.port" -> ExternalServicesConfig.Port,
    "microservice.services.api-subscription-fields.context" -> CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext,
    "microservice.services.google-analytics-sender.host" -> ExternalServicesConfig.Host,
    "microservice.services.google-analytics-sender.port" -> ExternalServicesConfig.Port,
    "microservice.services.google-analytics-sender.context" -> CustomsDeclarationsExternalServicesConfig.GoogleAnalyticsContext,
    "microservice.services.upscan-initiate.host" -> ExternalServicesConfig.Host,
    "microservice.services.upscan-initiate.port" -> ExternalServicesConfig.Port,
    "microservice.services.upscan-initiate.context" -> CustomsDeclarationsExternalServicesConfig.UpscanInitiateContext,
    "microservice.services.upscan-initiate.bearer-token" -> ExternalServicesConfig.AuthToken,
    "microservice.services.v2.upscan-initiate.host" -> ExternalServicesConfig.Host,
    "microservice.services.v2.upscan-initiate.port" -> ExternalServicesConfig.Port,
    "microservice.services.v2.upscan-initiate.context" -> CustomsDeclarationsExternalServicesConfig.UpscanInitiateContext,
    "microservice.services.v2.upscan-initiate.bearer-token" -> ExternalServicesConfig.AuthToken,
    "auditing.enabled" -> false,
    "auditing.consumer.baseUri.host" -> ExternalServicesConfig.Host,
    "auditing.consumer.baseUri.port" -> ExternalServicesConfig.Port,
    "microservice.services.customs-notification.host" -> ExternalServicesConfig.Host,
    "microservice.services.customs-notification.port" -> ExternalServicesConfig.Port,
    "microservice.services.customs-notification.context" -> "/customs-notification/notify",
    "microservice.services.customs-notification.bearer-token" -> CustomsDeclarationsExternalServicesConfig.CustomsNotificationAuthHeaderValue,
    "microservice.services.file-transmission.host" -> ExternalServicesConfig.Host,
    "microservice.services.file-transmission.port" -> ExternalServicesConfig.Port,
    "microservice.services.file-transmission.context" -> CustomsDeclarationsExternalServicesConfig.FileTransmissionContext
  )).build()

  protected def string2xml(s: String): Node = {
    val xml = try {
      XML.loadString(s)
    } catch {
      case NonFatal(thr) => fail("Not an xml: " + s, thr)
    }
    Utility.trim(xml)
  }

}