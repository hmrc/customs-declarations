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

import play.api.libs.json.Json
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._



class FileTransmissionModelSpec extends UnitSpec {
  private val expectedJson = """{
                               |  "batch" : {
                               |    "id" : "48400000-8cf0-11bd-b23e-10b96e4ef001",
                               |    "fileCount" : 2
                               |  },
                               |  "callbackUrl" : "https:/foo.com/callback",
                               |  "file" : {
                               |    "reference" : "31400000-8ce0-11bd-b23e-10b96e4ef00f",
                               |    "name" : "someFileN.ame",
                               |    "mimeType" : "application/pdf",
                               |    "checkSum" : "asdrfgvbhujk13579",
                               |    "location" : "https:/foo.com/location",
                               |    "sequenceNumber" : 1,
                               |    "size" : 1
                               |  },
                               |  "interface" : {
                               |    "name" : "interfaceName name",
                               |    "version" : "1.0"
                               |  },
                               |  "properties" : [ {
                               |    "name" : "p1",
                               |    "value" : "v1"
                               |  }, {
                               |    "name" : "p2",
                               |    "value" : "v2"
                               |  } ]
                               |}""".stripMargin


  private val batch = FileTransmissionBatch(BatchIdOne, 2)
  private val callBackUrl = new URL("https:/foo.com/callback")
  private val location = new URL("https:/foo.com/location")
  private val file = FileTransmissionFile(FileReferenceOne, name = "someFileN.ame", mimeType = "application/pdf", checkSum = "asdrfgvbhujk13579", location = location, FileSequenceNo(1))
  private val interface = FileTransmissionInterface("interfaceName name", "1.0")
  private val properties = Seq("p1" -> "v1", "p2" -> "v2").map(t => FileTransmissionProperty(t._1, t._2))
  private val fileTransmission = FileTransmission(batch, callBackUrl, file, interface, properties)

  "file transmission model" should {
    "serialise to Json" in {
      val actual = Json.prettyPrint(Json.toJson(fileTransmission))

      actual shouldBe expectedJson
    }
  }
}
