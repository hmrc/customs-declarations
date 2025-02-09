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

package unit.model.filetransmission

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.customs.declaration.model.filetransmission.FileTransmissionCallbackDecider
import util.FileTransmissionTestData._

class FileTransmissionNotificationModelSpec extends AnyWordSpecLike with Matchers {

  "FileTransmissionNotification model" can {
    "In happy path" should {
      "conditionally deserialize callback body as FileTransmissionSuccessNotification if outcome is SUCCESS" in {
        val JsSuccess(actual, _) = FileTransmissionCallbackDecider.parse(Json.parse(FileTransmissionSuccessNotificationPayload)): @unchecked

        actual shouldBe SuccessNotification
      }

      "conditionally deserialize callback body as FileTransmissionFailureNotification if outcome is FAILURE" in {
        val JsSuccess(actual, _) = FileTransmissionCallbackDecider.parse(Json.parse(FileTransmissionFailureNotificationPayload)): @unchecked

        actual shouldBe FailureNotification
      }
    }

    "In unhappy path" should {
      "return JsError when fileStatus is not SUCCESS or FAILURE" in {
        val JsError(list) = FileTransmissionCallbackDecider.parse(Json.parse(InvalidFileTransmissionNotificationPayload)): @unchecked

        val (path, _) = list.head
        path.toString shouldBe "/outcome"
      }

      "return JsError when payload is invalid" in {
        val JsError(list) = FileTransmissionCallbackDecider.parse(Json.parse("""{"foo": "bar"}""")): @unchecked

        val (path, _) = list.head
        path.toString shouldBe "/outcome"
      }
    }
  }


}
