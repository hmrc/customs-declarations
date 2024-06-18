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

package unit.connectors

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.customs.declaration.connectors.HeaderUtil
import uk.gov.hmrc.http.HeaderCarrier

class HeaderUtilSpec extends AnyWordSpecLike with Matchers with HeaderUtil {
  "HeaderUtil" should {
    "include Accept header when Gov-Test-Scenario header exists" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq(acceptHeader, govTestScenarioHeaders))
      val result = getCustomsApiStubExtraHeaders
      result.size shouldBe 2
      result.contains(acceptHeader) shouldBe true
      result.contains(govTestScenarioHeaders) shouldBe true
    }

    "return Gov-Test-Scenario header only if Accept header doesn't exists" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq(govTestScenarioHeaders))
      val result = getCustomsApiStubExtraHeaders
      result.size shouldBe 1
      result.contains(govTestScenarioHeaders)
    }

    "return empty seq when Gov-Test-Scenario header doesn't exists" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = Seq(acceptHeader))
      val result = getCustomsApiStubExtraHeaders
      result shouldBe Seq.empty[(String, String)]
    }
  }
  val acceptHeader = ("Accept", "application/vnd.hmrc.2.0+xml")
  val govTestScenarioHeaders = ("Gov-Test-Scenario", "DEFAULT")
}
