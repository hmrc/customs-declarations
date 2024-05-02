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

package unit.common

import cats.Applicative
import cats.data.Validated.{Invalid, Valid}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.config.{ConfigValidatedNelAdaptor, CustomsValidatedNel}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.UnitSpec

import scala.concurrent.duration.DurationInt

class ConfigValidatedNelAdaptorSpec extends UnitSpec with MockitoSugar with Matchers {

  private def appConfig(context: String): Config = ConfigFactory.parseString(
    s"""
      |root-string-key = root-string-value
      |root-int-key = 101
      |root-bool-key = true
      |root-duration-key = 60seconds
      |root-string-seq-key.0 = element1
      |root-string-seq-key.1 = element2
      |microservice {
      |  services {
      |    email {
      |      host = localhost
      |      port = 1111
      |      context = $context
      |      string-key = string-value
      |      int-key = 101
      |      bool-key = true
      |      duration-key = 60seconds
      |    }
      |  }
      |}
    """.stripMargin)

  private val validAppConfig: Config = appConfig("/context")
  private val prefixMissingContextConfig: Config = appConfig("context")
  private val nullContextConfig: Config = appConfig("null")
  
  private def testServicesConfig(configuration: Config) = new ServicesConfig(new Configuration(configuration)) {}
  
  private val configValidatedNelAdaptor = new ConfigValidatedNelAdaptor(testServicesConfig(validAppConfig), new Configuration(validAppConfig))
  private val prefixMissingContextNelAdaptor = new ConfigValidatedNelAdaptor(testServicesConfig(prefixMissingContextConfig), new Configuration(prefixMissingContextConfig))
  private val nullContextNelAdaptor = new ConfigValidatedNelAdaptor(testServicesConfig(nullContextConfig), new Configuration(nullContextConfig))

  "For root level config ConfigValidatedNelAdaptor" should {
    "return error when key not found" in {
      configValidatedNelAdaptor.root.string("ENSURE_KEY_NOT_FOUND") shouldBe Invalid("Could not find config key 'ENSURE_KEY_NOT_FOUND'").toValidatedNel
    }
    "return error when value is of wrong type" in {
      configValidatedNelAdaptor.root.int("root-string-key") shouldBe Invalid("Configuration error[String: 2: root-string-key has type STRING rather than NUMBER]").toValidatedNel
    }
    "read a string" in {
      configValidatedNelAdaptor.root.string("root-string-key") shouldBe Valid("root-string-value")
    }
    "read an Int" in {
      configValidatedNelAdaptor.root.int("root-int-key") shouldBe Valid(101)
    }
    "read a Boolean" in {
      configValidatedNelAdaptor.root.boolean("root-bool-key") shouldBe Valid(true)
    }
    "read a Duration" in {
      configValidatedNelAdaptor.root.duration("root-duration-key") shouldBe Valid(60.seconds)
    }
    "read a maybeString" in {
      configValidatedNelAdaptor.root.maybeString("root-string-key") shouldBe Valid(Some("root-string-value"))
    }
    "return None when a maybeString key is not found" in {
      configValidatedNelAdaptor.root.maybeString("ENSURE_KEY_NOT_FOUND") shouldBe Valid(None)
    }
    "read a maybeBoolean" in {
      configValidatedNelAdaptor.root.maybeBoolean("root-bool-key") shouldBe Valid(Some(true))
    }
    "return None when a maybeBoolean key is not found" in {
      configValidatedNelAdaptor.root.maybeBoolean("ENSURE_KEY_NOT_FOUND") shouldBe Valid(None)
    }
    "read a stringSeq" in {
      configValidatedNelAdaptor.root.stringSeq("root-string-seq-key") shouldBe Valid(Seq("element1", "element2"))
    }
    "return Nil when a stringSeq key is not found" in {
      configValidatedNelAdaptor.root.stringSeq("ENSURE_KEY_NOT_FOUND") shouldBe Valid(Nil)
    }
  }

  "For service level ConfigValidatedNelAdaptor" should {
    val emailNelAdaptor = configValidatedNelAdaptor.service("email")

    "return error when key not found for String" in {
      emailNelAdaptor.string("ENSURE_KEY_NOT_FOUND") shouldBe Invalid("Service configuration not found for key: email.ENSURE_KEY_NOT_FOUND").toValidatedNel
    }
    "return error when key not found for Int" in {
      emailNelAdaptor.int("ENSURE_KEY_NOT_FOUND") shouldBe Invalid("Service configuration not found for key: email.ENSURE_KEY_NOT_FOUND").toValidatedNel
    }
    "return error when key not found for Boolean" in {
      emailNelAdaptor.boolean("ENSURE_KEY_NOT_FOUND") shouldBe Invalid("Service configuration not found for key: email.ENSURE_KEY_NOT_FOUND").toValidatedNel
    }
    "return error when key not found for Duration" in {
      emailNelAdaptor.duration("ENSURE_KEY_NOT_FOUND") shouldBe Invalid("Service configuration not found for key: email.ENSURE_KEY_NOT_FOUND").toValidatedNel
    }
    "return error when value is of wrong type" in {
      emailNelAdaptor.int("string-key") shouldBe Invalid("Configuration error[String: 14: microservice.services.email.string-key has type STRING rather than NUMBER]").toValidatedNel
    }
    "return error when a field is null" in {
      nullContextNelAdaptor.service("email").serviceUrl shouldBe Invalid("Service configuration not found for key: email.context").toValidatedNel
    }
    "return error when context does not start with a '/'" in {
      prefixMissingContextNelAdaptor.service("email").serviceUrl shouldBe Invalid("For service 'email' context 'context' does not start with '/'").toValidatedNel
    }
    "read a string" in {
      emailNelAdaptor.string("string-key") shouldBe Valid("string-value")
    }
    "read an Int" in {
      emailNelAdaptor.int("int-key") shouldBe Valid(101)
    }
    "read a Boolean" in {
      emailNelAdaptor.boolean("bool-key") shouldBe Valid(true)
    }
    "read a Duration" in {
      emailNelAdaptor.duration("duration-key") shouldBe Valid(60.seconds)
    }
    "read a base url" in {
      emailNelAdaptor.baseUrl shouldBe Valid("http://localhost:1111")
    }
    "read a service url" in {
      emailNelAdaptor.serviceUrl shouldBe Valid("http://localhost:1111/context")
    }
  }

  "the cats library" should {
    // Note that mapping more than 5 fields caused a runtime problem using ScalaZ due to a clash of libraries on the class path.
    // So this test is to ensure we do not have the same problem with the cats library.
    "allow us to map a large number of ValidatedNel fields to a case class" in {
      case class FourteenFields(
       f1: String,f2: String,f3: String,f4: String,f5: String,f6: String,f7: String,f8: String,f9: String,f10: String,
       f11: String,f12: String,f13: String,f14: String)
      val x = configValidatedNelAdaptor.root.string("root-string-key")
      val y = "root-string-value"

      val actual = Applicative[CustomsValidatedNel].map14(x,x,x,x,x,x,x,x,x,x,x,x,x,x)(FourteenFields)

      actual shouldBe Valid(FourteenFields(y,y,y,y,y,y,y,y,y,y,y,y,y,y))
    }
  }
}
