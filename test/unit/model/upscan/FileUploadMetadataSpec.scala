/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.customs.declaration.model.upscan.FileUploadMetadata
import util.UnitSpec
import util.TestData.FileMetadataWithFileOne

class FileUploadMetadataSpec extends WordSpec with Matchers {
  private val jsonString = """{
                             |  "declarationId": "1",
                             |  "eori": "123",
                             |  "csId": "327d9145-4965-4d28-a2c5-39dedee50334",
                             |  "batchId": "48400000-8cf0-11bd-b23e-10b96e4ef001",
                             |  "fileCount": 1,
                             |  "createdAt": {"$date":1524562200000},
                             |  "files": [
                             |    {
                             |      "reference": "31400000-8ce0-11bd-b23e-10b96e4ef00f",
                             |      "maybeCallbackFields": {
                             |        "name": "name1",
                             |        "mimeType": "application/xml",
                             |        "checksum": "checksum1",
                             |        "uploadTimestamp": "2018-04-24T09:30:00Z",
                             |        "outboundLocation": "https://outbound.a.com"
                             |      },
                             |      "inboundLocation": "https://a.b.com",
                             |      "sequenceNumber": 1,
                             |      "size": 1,
                             |      "documentType": "Document Type 1"
                             |    }
                             |  ]
                             |}
                             |""".stripMargin

  private val json = Json.parse(jsonString)

  "FileUploadMetaData model" should {
    "serialise to Json" in {

      val actualJson = Json.toJson(FileMetadataWithFileOne)

      actualJson shouldBe json
    }

    "de-serialise from Json" in {

      val JsSuccess(actualMetaData, _) = Json.parse(jsonString).validate[FileUploadMetadata]

      actualMetaData shouldBe FileMetadataWithFileOne
    }
  }

}
