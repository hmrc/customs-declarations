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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import util.{CustomsDeclarationsExternalServicesConfig, ExternalServicesConfig}

import scala.util.control.NonFatal
import scala.xml.{Node, Utility, XML}

trait ComponentTestSpec extends FeatureSpec with GivenWhenThen with GuiceOneAppPerSuite
  with BeforeAndAfterAll with BeforeAndAfterEach {

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
    "microservice.services.api-subscription-fields.host" -> ExternalServicesConfig.Host,
    "microservice.services.api-subscription-fields.port" -> ExternalServicesConfig.Port,
    "microservice.services.api-subscription-fields.context" -> CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext,
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
    "microservice.services.customs-notification.bearer-token" -> CustomsDeclarationsExternalServicesConfig.CustomsNotificationAuthHeaderValue
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