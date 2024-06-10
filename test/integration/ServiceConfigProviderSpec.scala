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

package integration

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.customs.declaration.config.{InvalidEnvironmentException, ServiceConfigProvider}
import util.DynamicServiceTestData

class ServiceConfigProviderSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with DynamicServiceTestData {

  private val serviceConfigProvider = new ServiceConfigProvider(app.injector.instanceOf[ServicesConfig])

  override implicit lazy val app: Application = configuredApplication

  override def beforeEach(): Unit = {
    serviceConfigProvider.reset()
  }

  "changing service configuration to an environment" when {
    "no configuration exists for the service in the specified environment" should {

      val invalidEnvironment = "invalid-environment"

      "throw an InvalidEnvironmentException" in {
        val thrown = intercept[InvalidEnvironmentException]{
          serviceConfigProvider.setEnvironment(validService, invalidEnvironment)
        }

        thrown.getMessage shouldBe s"No configuration was found for service $validService in environment $invalidEnvironment"
      }
    }

    "configuration exists for the service in the specified environment" should {
      "set the environment for the service" in {
        serviceConfigProvider.setEnvironment(validService, validEnvironment)
      }
    }
  }

  "getting service configuration" when {
    "no environment has been set" should {
      "retrieve the default configuration" in {
        val res = serviceConfigProvider.getConfig(validService)

        res.environment shouldBe defaultEnvironment
        res.url shouldBe s"http://$validServiceHost:$validServicePort$validServiceContext"
        res.bearerToken shouldBe Some(validServiceBearer)
      }
    }

    "environment has been set" should {
      "retrieve the configuration for the environment" in {
        serviceConfigProvider.setEnvironment(validService, validEnvironment)
        val res = serviceConfigProvider.getConfig(validService)

        res.environment shouldBe validEnvironment
        res.url shouldBe s"http://$validEnvironmentHost:$validEnvironmentPort$validEnvironmentContext"
        res.bearerToken shouldBe Some(validEnvironmentBearer)
      }
    }

    "environment has been overridden and then set to default" should {
      "retrieve the default configuration" in {
        serviceConfigProvider.setEnvironment(validService, validEnvironment)
        serviceConfigProvider.setEnvironment(validService, defaultEnvironment)

        val res = serviceConfigProvider.getConfig(validService)

        res.environment shouldBe defaultEnvironment
        res.url shouldBe s"http://$validServiceHost:$validServicePort$validServiceContext"
        res.bearerToken shouldBe Some(validServiceBearer)
      }
    }
  }
}
