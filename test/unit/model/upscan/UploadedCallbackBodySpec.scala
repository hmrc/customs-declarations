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

package unit.model.upscan

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import util.UpscanNotifyTestData._
import play.api.libs.json._
import uk.gov.hmrc.customs.declaration.model.upscan.UploadedReadyCallbackBody._

class UploadedCallbackBodySpec extends  AnyWordSpecLike with Matchers {
  "UploadedCallbackBody model" can {
    "In Happy Path" should {
      "conditionally de-serialisation callback body as UploadedFailedCallbackBody if fileStatus is READY" in {
        val JsSuccess(actual, _) = parse(readyJson()): @unchecked

        actual shouldBe ReadyCallbackBody
      }

      "conditionally de-serialise callback body as UploadedReadyCallbackBody if fileStatus is FAILED" in {
        val JsSuccess(actual, _) = parse(FailedJson): @unchecked

        actual shouldBe FailedCallbackBody
      }
    }
    "In Un-Happy Path" should {
      "return JsError when fileStatus is not READY or FAILED" in {
        val JsError(list) = parse(FailedJsonWithInvalidFileStatus): @unchecked

        val (path, _) = list.head
        path.toString shouldBe "/fileStatus"
      }

      "return JsError when payload is invalid" in {
        val JsError(list) = parse(Json.parse("""{"foo": "bar"}""")): @unchecked

        val (path, _) = list.head
        path.toString shouldBe "/fileStatus"
      }
    }
  }

}
