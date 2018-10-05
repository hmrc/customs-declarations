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

package util

import java.net.URL

import uk.gov.hmrc.customs.declaration.model._
import util.TestData.{BatchIdOne, FileReferenceOne}

object FileTransmissionTestData {

  val FileTransmissionBatchOne = FileTransmissionBatch(BatchIdOne, 2)
  val FileTransmissionCallBackUrl = new URL("https:/foo.com/callback")
  val FileTransmissionLocation = new URL("https:/foo.com/location")
  val FileTransmissionFileOne = FileTransmissionFile(FileReferenceOne, name = "someFileN.ame", mimeType = "application/pdf", checkSum = "asdrfgvbhujk13579", location = FileTransmissionLocation, FileSequenceNo(1))
  val FileTransmissionInterfaceOne = FileTransmissionInterface("interfaceName name", "1.0")
  val FileTransmissionProperties = Seq("p1" -> "v1", "p2" -> "v2").map(t => FileTransmissionProperty(name = t._1, value = t._2))
  val FileTransmissionRequest = FileTransmission(FileTransmissionBatchOne, FileTransmissionCallBackUrl, FileTransmissionFileOne, FileTransmissionInterfaceOne, FileTransmissionProperties)

  val FileTransmissionRequestJsonString = """{
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
}
