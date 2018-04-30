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

package unit.services

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.customs.api.common.config.{ConfigValidationNelAdaptor, ServicesConfig}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{ApiDefinitionConfig, CustomsEnrolmentConfig}
import uk.gov.hmrc.customs.declaration.services.CustomsConfigService
import uk.gov.hmrc.play.test.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier

class CustomsConfigServiceSpec extends UnitSpec with MockitoSugar {
  private val validAppConfig: Config = ConfigFactory.parseString(
    """
      |customs = {
      |   definition = {
      |     api-scope = "write:customs-declaration"
      |   }
      |
      |   enrolment = {
      |     name = "HMRC-CUS-ORG"
      |     eori-identifier = "EORINumber"
      |   }
      |}
    """.stripMargin)

  private val emptyAppConfig: Config = ConfigFactory.parseString("")

  private val validServicesConfiguration = Configuration(validAppConfig)
  private val emptyServicesConfiguration = Configuration(emptyAppConfig)

  private val mockLogger = mock[DeclarationsLogger]

  private def customsConfigService(conf: Configuration) =
    new CustomsConfigService(new ConfigValidationNelAdaptor(testServicesConfig(conf), conf), mockLogger)

  "CustomsConfigService" should {
    "return config as object model when configuration is valid" in {
      val configService = customsConfigService(validServicesConfiguration)

      configService.apiDefinitionConfig shouldBe ApiDefinitionConfig("write:customs-declaration")
      configService.customsEnrolmentConfig shouldBe CustomsEnrolmentConfig("HMRC-CUS-ORG", "EORINumber")
    }

    "throw an exception when configuration is invalid, that contains AGGREGATED error messages" in {
      val expectedErrorMessage =
        """
          |Could not find config key 'customs.definition.api-scope'
          |Could not find config key 'customs.enrolment.name'
          |Could not find config key 'customs.enrolment.eori-identifier'""".stripMargin

      val caught = intercept[IllegalStateException](customsConfigService(emptyServicesConfiguration))
      caught.getMessage shouldBe expectedErrorMessage

      PassByNameVerifier(mockLogger, "errorWithoutRequestContext")
        .withByNameParam[String](expectedErrorMessage)
        .verify()
    }
  }

  private def testServicesConfig(configuration: Configuration) = new ServicesConfig(configuration, mock[Environment]) {
    override val mode = play.api.Mode.Test
  }

}
