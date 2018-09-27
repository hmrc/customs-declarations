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

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.BatchFileMetadataWithFileOne

class BatchFileUploadMetadataSpec extends UnitSpec {
  private val jsonString = """{
                     |  "declarationId": "1",
                     |  "eori": "123",
                     |  "csId": "327d9145-4965-4d28-a2c5-39dedee50334",
                     |  "batchId": "48400000-8cf0-11bd-b23e-10b96e4ef001",
                     |  "fileCount": 1,
                     |  "files": [
                     |    {
                     |      "reference": "38400000-8ce0-11bd-b23e-10b96e4ef00f",
                     |      "name": "name1",
                     |      "mimeType": "application/xml",
                     |      "checksum": "checksum1",
                     |      "location": "https://a.b.com",
                     |      "sequenceNumber": 1,
                     |      "size": 1,
                     |      "documentType": "Document Type 1"
                     |    }
                     |  ]
                     |}""".stripMargin

  private val json = Json.parse(jsonString)

  "BatchFileUploadMetaData model" should {
    "serialise to Json" in {

      val actualJson = Json.toJson(BatchFileMetadataWithFileOne)

      actualJson shouldBe json
    }

    "de-serialise from Json" in {

      val JsSuccess(actualMetaData, _) = Json.parse(jsonString).validate[BatchFileUploadMetadata]

      actualMetaData shouldBe BatchFileMetadataWithFileOne
    }
  }

}
