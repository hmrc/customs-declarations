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

package unit.controllers.dynamicservice

import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.config.{InvalidEnvironmentException, ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.controllers.dynamicservice.DynamicServiceConfigurationController
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import util.UnitSpec

class DynamicServiceConfigurationControllerSpec extends UnitSpec with Matchers with MockitoSugar {

  private val validService           = "service"
  private val invalidService         = "invalid-service"
  private val environment            = "environment"
  private val invalidEnvironment     = "invalid-environment"
  private val config                 = mock[ServiceConfigProvider]
  private val controllerComponents   = Helpers.stubControllerComponents()
  private val controller             = new DynamicServiceConfigurationController(config, controllerComponents)
  private val validBody              = Json.parse(s"""{"environment": "$environment"}""")
  private val invalidEnvironmentBody = Json.parse(s"""{"environment": "$invalidEnvironment"}""")

  "setting the environment for a service" when {

    "no environment is provided in the body" should {
      val res = postSetConfiguration(Json.parse("{}"))

      "result in a bad request error" in {
        status(res) shouldBe BAD_REQUEST
      }
    }

    "the service does not have a configuration entry for the environment" should {
      when(config.setEnvironment(validService, "invalid-environment")).thenThrow(new InvalidEnvironmentException)
      val res = postSetConfiguration(invalidEnvironmentBody)

      "result in a not found error" in {
        status(res) shouldBe NOT_FOUND
      }

      "inform client of the reason for failure" in {
        contentAsString(res) shouldBe s"No configuration was found for service $validService in environment $invalidEnvironment"
      }
    }

    "a configuration entry exists for the service and environment" should {
      val res = postSetConfiguration(validBody)

      "result in an OK response" in {
        status(res) shouldBe OK
      }

      "inform client that the service has been pointed to the environment" in {
        contentAsString(res) shouldBe s"The service $validService is now configured to use the $environment environment"
      }
    }
  }

  "getting the current configuration for a service" when {
    "no configuration exists for the service" should {
      when(config.getConfig(invalidService)).thenThrow(new RuntimeException)
      val res = controller.getConfigurationForService(invalidService).apply(FakeRequest(GET, "/"))

      "return not found" in {
        status(res) shouldBe NOT_FOUND
      }
    }

    "configuration exists for the service" should {
      val url = "URL"
      val token = Some("token")

      when(config.getConfig(validService)).thenReturn(ServiceConfig(url, token, environment))
      val res = controller.getConfigurationForService(validService).apply(FakeRequest(GET, "/"))

      "return OK" in {
        status(res) shouldBe OK
      }

      "inform the client of the configuration details" in {
        contentAsJson(res) shouldBe Json.parse{
          s"""|{"service": "$validService",
              |"environment": "$environment",
              |"url": "$url",
              |"bearerToken": "${token.get}"}
         """.stripMargin}
      }
    }
  }

  private def postSetConfiguration(body: JsValue) = {
    controller
      .setConfigurationForService(validService)
      .apply(FakeRequest(POST, "/").withJsonBody(body))
  }
}
