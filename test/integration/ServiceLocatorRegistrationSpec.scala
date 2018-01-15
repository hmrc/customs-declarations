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

package integration

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.MixedPlaySpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.customs.api.common.domain.Registration
import util.ExternalServicesConfig
import util.externalservices.RegistrationService

class ServiceLocatorRegistrationSpec extends MixedPlaySpec
  with BeforeAndAfterEach with BeforeAndAfterAll with RegistrationService {

  private val expectedRegistration = Registration(
    "customs-declarations",
    "http://customs-declarations-host",
    Some(Map("third-party-api" -> "true")))

  override def beforeEach() {
    startMockServer()
    registrationServiceIsRunning()
  }

  override def afterEach() {
    stopMockServer()
  }

  def createTestConfiguration(enableServiceLocator: Boolean): Map[String, Any] = {
    Map(
      "microservice.services.service-locator.host" -> ExternalServicesConfig.Host,
      "microservice.services.service-locator.port" -> ExternalServicesConfig.Port,
      "microservice.services.service-locator.enabled" -> enableServiceLocator
    )}

  private def app(enableServiceLocator: Boolean): Application =
    new GuiceApplicationBuilder().configure(createTestConfiguration(enableServiceLocator)).build()

  "Single WCO Compliant Declaration Microservice" should {

    "not register itself in service locator microservice in start up when is disabled" in
      new App(app(enableServiceLocator = false)) {

        eventually {
          noRequestWasMadeToRegistrationService()
        }
      }

    "register itself in service locator microservice in start up when is enabled" in
      new App(app(enableServiceLocator = true)) {

        eventually {
          verifyRegistrationServiceWasCalledFor(Json.toJson(expectedRegistration))
        }
      }
  }
}

