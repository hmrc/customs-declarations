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

package util

import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.filetransmission._
import util.TestData.{BatchIdOne, FileReferenceOne, InitiateDate}

import java.net.URL

object FileTransmissionTestData {

  val FileTransmissionBatchOne = FileTransmissionBatch(BatchIdOne, 2)
  val FileTransmissionCallBackUrl = new URL("https:/foo.com/callback")
  val FileTransmissionLocation = new URL("https:/foo.com/location")
  val FileTransmissionFileOne = FileTransmissionFile(FileReferenceOne, name = "someFileN.ame", mimeType = "application/pdf", checksum = "asdrfgvbhujk13579", location = FileTransmissionLocation, FileSequenceNo(1), uploadTimestamp = InitiateDate)
  val FileTransmissionInterfaceOne = FileTransmissionInterface("interfaceName name", "1.0")
  val FileTransmissionProperties = Seq("p1" -> "v1", "p2" -> "v2").map(t => FileTransmissionProperty(name = t._1, value = t._2))
  val FileTransmissionRequest = FileTransmission(FileTransmissionBatchOne, FileTransmissionCallBackUrl, FileTransmissionFileOne, FileTransmissionInterfaceOne, FileTransmissionProperties)

  val SuccessNotification = FileTransmissionSuccessNotification(FileReferenceOne, BatchIdOne, FileTransmissionSuccessOutcome)
  val FailureNotification = FileTransmissionFailureNotification(FileReferenceOne, BatchIdOne, FileTransmissionFailureOutcome, "error text")

  val FileTransmissionRequestJsonString = """{
       |  "interface" : {
       |    "name" : "interfaceName name",
       |    "version" : "1.0"
       |  },
       |  "batch" : {
       |    "id" : "48400000-8cf0-11bd-b23e-10b96e4ef001",
       |    "fileCount" : 2
       |  },
       |  "file" : {
       |    "reference" : "31400000-8ce0-11bd-b23e-10b96e4ef00f",
       |    "name" : "someFileN.ame",
       |    "location" : "https:/foo.com/location",
       |    "size" : 1,
       |    "mimeType" : "application/pdf",
       |    "checksum" : "asdrfgvbhujk13579",
       |    "uploadTimestamp" : "2018-04-24T09:30:00Z",
       |    "sequenceNumber" : 1
       |  },
       |  "properties" : [ {
       |    "name" : "p1",
       |    "value" : "v1"
       |  }, {
       |    "name" : "p2",
       |    "value" : "v2"
       |  } ],
       |  "callbackUrl" : "https:/foo.com/callback"
}""".stripMargin

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
      <Root xmlns="hmrc:fileupload">
        <FileReference>31400000-8ce0-11bd-b23e-10b96e4ef00f</FileReference>
        <BatchId>48400000-8cf0-11bd-b23e-10b96e4ef001</BatchId>
        <FileName>name1</FileName>
        <Outcome>SUCCESS</Outcome>
        <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
      </Root>

  val FileTransmissionSuccessCustomsNotificationXmlWithoutFilename =
    <Root xmlns="hmrc:fileupload">
        <FileReference>31400000-8ce0-11bd-b23e-10b96e4ef00f</FileReference>
        <BatchId>48400000-8cf0-11bd-b23e-10b96e4ef001</BatchId>
        <Outcome>SUCCESS</Outcome>
        <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
      </Root>

  val FileTransmissionFailureCustomsNotificationXml =
      <Root xmlns="hmrc:fileupload">
        <FileReference>31400000-8ce0-11bd-b23e-10b96e4ef00f</FileReference>
        <BatchId>48400000-8cf0-11bd-b23e-10b96e4ef001</BatchId>
        <FileName>name1</FileName>
        <Outcome>FAILURE</Outcome>
        <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
      </Root>

  val FileTransmissionFailureCustomsNotificationXmlWithoutFilename =
    <Root xmlns="hmrc:fileupload">
        <FileReference>31400000-8ce0-11bd-b23e-10b96e4ef00f</FileReference>
        <BatchId>48400000-8cf0-11bd-b23e-10b96e4ef001</BatchId>
        <Outcome>FAILURE</Outcome>
        <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
      </Root>

  val invalidJson = """{"this" is not valid json"""

  val InternalErrorResponseJson = """{"code":"INTERNAL_SERVER_ERROR","message":"Internal server error"}"""
  val BadRequestErrorResponseInvalidOutcome = """{"code":"BAD_REQUEST","message":"Invalid file upload outcome"}"""
  val BadRequestErrorResponseInvalidJson = """{"code":"BAD_REQUEST","message":"Invalid JSON payload"}"""
  val FileTransmissionClientSubscriptionIdErrorJson = """{"code":"BAD_REQUEST","message":"Invalid clientSubscriptionId"}"""

}
