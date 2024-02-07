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

package component
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.DynamicServiceTestData

class DynamicServiceConfigurationSpec extends AnyFeatureSpec with GivenWhenThen with Matchers with GuiceOneAppPerSuite with BeforeAndAfterEach with DynamicServiceTestData {

  override implicit lazy val app: Application = configuredApplication

  override def beforeEach(): Unit ={
    val request = FakeRequest(POST, s"/$validService/configuration").
      withJsonBody(Json.parse("""{"environment": "default"}"""))

    route(app, request)
  }

  Feature("Changing environment for service") {

    Scenario("Environment is not found in configuration for service") {
      Given("A service does not have a configuration entry for the specified environment")
      val environment = "missingenv"

      When("A request to change the environment is received")
      val body = Json.parse(s"""{"environment": "$environment"}""")
      val request = FakeRequest(POST, s"/$validService/configuration").withJsonBody(body)
      val res = route(app, request).get

      Then("An error is returned")
      status(res) shouldBe NOT_FOUND
      contentAsString(res) shouldBe s"No configuration was found for service $validService in environment $environment"
    }

    Scenario("Environment is found in configuration for service") {
      Given("A service has a configuration for the specified environment")

      When("A request to change the environment is received")
      val body = Json.parse(s"""{"environment": "$validEnvironment"}""")
      val request = FakeRequest(POST, s"/$validService/configuration").withJsonBody(body)
      val res = route(app, request).get

      Then("An OK response is returned")
      status(res) shouldBe OK
      contentAsString(res) shouldBe s"The service $validService is now configured to use the $validEnvironment environment"
    }
  }

  Feature("Getting current service configuration") {
    Scenario("Service does not have a configuration entry") {
      Given("A service does not have a configuration entry")

      When("Getting the current configuration")
      val request = FakeRequest(GET, "/invalid-service/configuration")
      val res = route(app, request).get

      Then("A not found response is returned")
      status(res) shouldBe NOT_FOUND
    }

    Scenario("Service has a configuration entry") {
      Given("A service has a configuration entry")

      When("Getting the current configuration")
      val request = FakeRequest(GET, s"/$validService/configuration")
      val res = route(app, request).get

      Then("An OK response is returned")
      status(res) shouldBe OK

      And("The service config details are returned")
      contentAsJson(res) shouldBe Json.parse {
        s"""|{"service": "$validService",
            |"environment": "default",
            |"url": "http://$validServiceHost:$validServicePort$validServiceContext",
            |"bearerToken": "$validServiceBearer"}
         """.stripMargin
      }
    }

    Scenario("Service without bearer token") {
      Given("A service has a configuration entry with no bearer token")

      When("Getting the current configuration")
      val request = FakeRequest(GET, s"/$noBearerService/configuration")
      val res = route(app, request).get

      Then("An OK response is returned")
      status(res) shouldBe OK

      And("The service config details are returned")
      contentAsJson(res) shouldBe Json.parse {
        s"""|{"service": "$noBearerService",
            |"environment": "default",
            |"url": "http://$noBearerServiceHost:$noBearerServicePort$noBearerServiceContext",
            |"bearerToken": ""}
         """.stripMargin
      }
    }
  }
}
