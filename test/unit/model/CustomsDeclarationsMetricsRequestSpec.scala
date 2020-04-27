/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.model

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.customs.declaration.model.CustomsDeclarationsMetricsRequest
import uk.gov.hmrc.customs.declaration.model.CustomsDeclarationsMetricsRequest._
import util.UnitSpec
import util.CustomsDeclarationsMetricsTestData._
import util.TestData.conversationId

class CustomsDeclarationsMetricsRequestSpec extends UnitSpec {


  private val expectedJson: JsValue = Json.parse("""
                                        |{
                                        |  "eventType" : "DECLARATION",
                                        |  "conversationId" : "38400000-8cf0-11bd-b23e-10b96e4ef00d",
                                        |  "eventStart" : "2015-11-30T23:45:59Z[UTC]",
                                        |  "eventEnd" : "2015-11-30T23:46:01Z[UTC]"
                                        |}
                                      """.stripMargin)

  "CustomsDeclarationsMetricsRequest model" should {
    "serialise to Json" in {
      val request = CustomsDeclarationsMetricsRequest("DECLARATION", conversationId, EventStart, EventEnd)
      val actualJson: JsValue = Json.toJson(request)

      actualJson shouldBe expectedJson
    }

  }
}
