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

package unit.model

import java.net.URL
import java.time.Instant

import util.TestData
import play.api.libs.json._
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.play.test.UnitSpec

class UploadedCallbackBodySpec extends UnitSpec {
  private val readyJson = """{
                            |      "reference" : "31400000-8ce0-11bd-b23e-10b96e4ef00f",
                            |      "downloadUrl" : "http://remotehost/bucket/123",
                            |      "fileStatus": "READY",
                            |      "uploadDetails": {
                            |        "uploadTimestamp": "2018-04-24T09:30:00Z",
                            |        "checksum": "1a2b3c4d5e",
                            |         "fileMimeType": "application/pdf",
                            |         "fileName": "test.pdf"
                            |      }
                            |}
                            |""".stripMargin

  private val failedJson = """{
                             |       "reference" : "31400000-8ce0-11bd-b23e-10b96e4ef00f",
                             |       "fileStatus" : "FAILED",
                             |       "failureDetails" : {
                             |         "failureReason" : "QUARANTINE",
                             |         "message" : "This file has a virus"
                             |       }
                             |}""".stripMargin

  private val failedJsonWithInvalidFileStatus = """{
                             |       "reference" : "31400000-8ce0-11bd-b23e-10b96e4ef00f",
                             |       "fileStatus" : "INVALID_FILE_STATUS",
                             |       "failureDetails" : {
                             |         "failureReason" : "QUARANTINE",
                             |         "message" : "This file has a virus"
                             |       }
                             |}""".stripMargin

  private val downloadUrl = new URL("http://remotehost/bucket/123")
  private val initiateDate = Instant.parse("2018-04-24T09:30:00Z")
  private val uploadDetails = UploadedDetails("test.pdf", "application/pdf", initiateDate, "1a2b3c4d5e")
  private val readyCallbackBody = UploadedReadyCallbackBody(TestData.FileReferenceOne, downloadUrl, ReadyFileStatus, uploadDetails)
  private val errorDetails = UploadedErrorDetails("QUARANTINE", "This file has a virus")
  private val failedCallbackBody = UploadedFailedCallbackBody(TestData.FileReferenceOne, FailedFileStatus, errorDetails)

  "UploadedCallbackBody model" can {
    "In Happy Path" should {
      "conditionally de-serialise callback body as UploadedReadyCallbackBody if fileStatus is READY" in {
        val JsSuccess(actual, _) = UploadedFailedCallbackBody.parse(Json.parse(failedJson))

        actual shouldBe failedCallbackBody
      }

      "conditionally de-serialisation callback body as UploadedFailedCallbackBody if fileStatus is READY" in {
        val JsSuccess(actual, _) = UploadedFailedCallbackBody.parse(Json.parse(readyJson))

        actual shouldBe readyCallbackBody
      }
    }
    "In Un-Happy Path" should {
      "return JsError when fileStatus is not READY or FAILED" in {
        val JsError(list) = UploadedFailedCallbackBody.parse(Json.parse(failedJsonWithInvalidFileStatus))

        val (path, _) = list.head
        path.toString shouldBe "/fileStatus"
      }

      "return JsError when payload is invalid" in {
        val JsError(list) = UploadedFailedCallbackBody.parse(Json.parse("""{"foo": "bar"}"""))

        val (path, _) = list.head
        path.toString shouldBe "/fileStatus"
      }
    }
  }


}
