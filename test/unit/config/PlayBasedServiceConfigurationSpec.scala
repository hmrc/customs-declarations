/*
 * Copyright 2026 HM Revenue & Customs
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

package unit.config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.config.PlayBasedServiceConfiguration

import scala.concurrent.duration.*

class PlayBasedServiceConfigurationSpec extends AnyWordSpecLike with Matchers {

  private val validConfig = Configuration(
    "mdg.endpoint"                   -> "http://localhost:9576/mdg-stub/request",
    "mdg.authorizationToken"         -> "mockToken",
    "queuePollingInterval"           -> "500 milliseconds",
    "queueRetryAfterFailureInterval" -> "10 seconds",
    "inFlightLockDuration"           -> "5 minutes",
    "initialBackoffAfterFailure"     -> "1 minute",
    "deliveryWindowDuration"         -> "4 hours"
  )

  "PlayBasedServiceConfiguration" should {

    "read all values from configuration" in {
      val config = new PlayBasedServiceConfiguration(validConfig)

      config.mdgEndpoint shouldBe "http://localhost:9576/mdg-stub/request"
      config.mdgAuthorizationToken shouldBe "mockToken"
      config.queuePollingInterval shouldBe 500.millis
      config.queueRetryAfterFailureInterval shouldBe 10.seconds
      config.inFlightLockDuration shouldBe 5.minutes
      config.initialBackoffAfterFailure shouldBe 1.minute
      config.defaultDeliveryWindowDuration shouldBe 4.hours
    }

    "throw an IllegalStateException when a required key is missing" in {
      val config = new PlayBasedServiceConfiguration(Configuration.empty)

      intercept[IllegalStateException](config.mdgEndpoint).getMessage shouldBe "Configuration key not found: mdg.endpoint"
      intercept[IllegalStateException](config.queuePollingInterval).getMessage shouldBe "Configuration key not found: queuePollingInterval"
    }
  }
}
