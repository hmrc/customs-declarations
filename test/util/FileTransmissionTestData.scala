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

  val SuccessNotification = FileTransmissionSuccessNotification(FileReferenceOne, BatchIdOne, FileTransmissionSuccessOutcome)
  val FailureNotification = FileTransmissionFailureNotification(FileReferenceOne, BatchIdOne, FileTransmissionFailureOutcome, "error text")

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

  val FileTransmissionSuccessNotificationPayload = s"""
       |{
       |  "fileReference":"31400000-8ce0-11bd-b23e-10b96e4ef00f",
       |  "batchId":"48400000-8cf0-11bd-b23e-10b96e4ef001",
       |  "outcome":"SUCCESS"
       |}
    """.stripMargin

  val FileTransmissionFailureNotificationPayload = s"""
       |{
       |  "fileReference":"31400000-8ce0-11bd-b23e-10b96e4ef00f",
       |  "batchId":"48400000-8cf0-11bd-b23e-10b96e4ef001",
       |  "outcome":"FAILURE",
       |  "errorDetails":"error text"
       |}
    """.stripMargin

  val InvalidFileTransmissionNotificationPayload = s"""
       |{
       |  "fileReference":"31400000-8ce0-11bd-b23e-10b96e4ef00f",
       |  "batchId":"48400000-8cf0-11bd-b23e-10b96e4ef001",
       |  "outcome":"INVALID-OUTCOME",
       |  "errorDetails":"Some error details text"
       |}
    """.stripMargin

  val FileTransmissionSuccessCustomsNotificationXml =
    <Root>
        <FileReference>31400000-8ce0-11bd-b23e-10b96e4ef00f</FileReference>
        <BatchId>48400000-8cf0-11bd-b23e-10b96e4ef001</BatchId>
        <Outcome>SUCCESS</Outcome>
        <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
      </Root>

  val FileTransmissionFailureCustomsNotificationXml =
    <Root>
        <FileReference>31400000-8ce0-11bd-b23e-10b96e4ef00f</FileReference>
        <BatchId>48400000-8cf0-11bd-b23e-10b96e4ef001</BatchId>
        <Outcome>FAILURE</Outcome>
        <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
      </Root>

  val invalidJson = """{"this" is not valid json"""

  val InternalErrorResponseJson = """{"code":"INTERNAL_SERVER_ERROR","message":"Internal server error"}"""
  val BadRequestErrorResponseInvalidOutcome = """{"code":"BAD_REQUEST","message":"Invalid file upload outcome"}"""
  val BadRequestErrorResponseInvalidJson = """{"code":"BAD_REQUEST","message":"Invalid JSON payload"}"""

}
