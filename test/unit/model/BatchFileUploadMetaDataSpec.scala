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
import java.util.UUID
import java.util.UUID.fromString

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.play.test.UnitSpec

class BatchFileUploadMetaDataSpec extends UnitSpec {
  private val batchIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  private val batchIdUuid: UUID = fromString(batchIdValue)
  private val metadata = BatchFileUploadMetaData(DeclarationId("1"), Eori("123"), csId = SubscriptionFieldsId("123"), batchId = batchIdUuid, fileCount = 1, Seq(
    BatchFile(reference = "ref1", name = "name1", mimeType = "application/xml", checksum = "checksum1",
      location = new URL("https://a.b.com"), sequenceNumber = 1, size = 1, documentType = DocumentationType("Document Type 1"))
  ))
  private val jsonString = """{
                     |  "declarationId": "1",
                     |  "eori": "123",
                     |  "csId": "123",
                     |  "batchId": "38400000-8cf0-11bd-b23e-10b96e4ef00d",
                     |  "fileCount": 1,
                     |  "files": [
                     |    {
                     |      "reference": "ref1",
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

      val actualJson = Json.toJson(metadata)

      actualJson shouldBe json
    }

    "de-serialise from Json" in {

      val JsSuccess(actualMetaData, _) = Json.parse(jsonString).validate[BatchFileUploadMetaData]

      actualMetaData shouldBe metadata
    }
  }

}
