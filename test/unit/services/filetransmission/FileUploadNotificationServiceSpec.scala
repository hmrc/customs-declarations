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

package unit.services.filetransmission

import java.util.UUID
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => ameq}
import org.mockito.Mockito._
import org.scalatest.{Assertion, Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Reads
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.upscan.FileUploadCustomsNotificationConnector
import uk.gov.hmrc.customs.declaration.model.ConversationId
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.filetransmission.{FileTransmissionFailureOutcome, FileTransmissionNotification, FileTransmissionSuccessNotification, FileTransmissionSuccessOutcome}
import uk.gov.hmrc.customs.declaration.model.upscan.{BatchId, FileReference}
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.upscan.{CallbackToXmlNotification, FileUploadCustomsNotification, FileUploadNotificationService}
import unit.services.filetransmission.ExampleFileTransmissionStatus.ExampleFileTransmissionStatus
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.TestData
import util.TestData._
import util.XmlOps._

import scala.concurrent.Future
import scala.xml.NodeSeq

object ExampleFileTransmissionStatus extends Enumeration {
  type ExampleFileTransmissionStatus = Value
  val SUCCESS, FAILURE = Value

  implicit val fileTransmissionStatusReads = Reads.enumNameReads(ExampleFileTransmissionStatus)
}

case class ExampleFileTransmissionNotification(fileReference: FileReference,
                                               batchId: BatchId,
                                               fileTransmissionStatus: ExampleFileTransmissionStatus,
                                               errorDetails: Option[String])

class FileUploadNotificationServiceSpec extends WordSpec with MockitoSugar with Matchers{

  trait SetUp {
    private[FileUploadNotificationServiceSpec] implicit val ec = Helpers.stubControllerComponents().executionContext
    private[FileUploadNotificationServiceSpec] val mockFileUploadMetadataRepo = mock[FileUploadMetadataRepo]
    private[FileUploadNotificationServiceSpec] val mockNotificationConnector = mock[FileUploadCustomsNotificationConnector]
    private[FileUploadNotificationServiceSpec] val mockDeclarationsLogger = mock[CdsLogger]
    private[FileUploadNotificationServiceSpec] val service = new FileUploadNotificationService(mockFileUploadMetadataRepo, mockNotificationConnector, mockDeclarationsLogger)
    private[FileUploadNotificationServiceSpec] val expectedSuccessXml =
      <Root xmlns="hmrc:fileupload">
        <FileReference>{FileReferenceOne}</FileReference>
        <BatchId>{BatchIdOne}</BatchId>
        <FileName>name1</FileName>
        <Outcome>SUCCESS</Outcome>
        <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
      </Root>
    private[FileUploadNotificationServiceSpec] val expectedFailureXml =
      <Root xmlns="hmrc:fileupload">
        <FileReference>{FileReferenceOne}</FileReference>
        <BatchId>{BatchIdOne}</BatchId>
        <Outcome>FAILURE</Outcome>
        <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
      </Root>

    private[FileUploadNotificationServiceSpec] def verifyNotificationConnectorCalledWithXml(xml: NodeSeq): Assertion = {
      val captor: ArgumentCaptor[FileUploadCustomsNotification] = ArgumentCaptor.forClass(classOf[FileUploadCustomsNotification])
      verify(mockNotificationConnector).send(captor.capture())
      val actual: FileUploadCustomsNotification = captor.getValue
      actual.clientSubscriptionId shouldBe subscriptionFieldsId
      actual.conversationId shouldBe FileReferenceOne.value
      stringToXml(actual.payload.toString) shouldBe stringToXml(xml.toString)
    }

    private[FileUploadNotificationServiceSpec] implicit val hasConversationId: HasConversationId = new HasConversationId {
      override val conversationId: ConversationId = TestData.conversationId
    }

    private[FileUploadNotificationServiceSpec] def fileRefEq(fRef: FileReference) = ameq[UUID](fRef.value).asInstanceOf[FileReference]

    private[FileUploadNotificationServiceSpec] implicit val toXml: CallbackToXmlNotification[FileTransmissionNotification] =
      new uk.gov.hmrc.customs.declaration.services.filetransmission.FileTransmissionCallbackToXmlNotification()
  }

  private val successCallbackPayload: FileTransmissionNotification =
    FileTransmissionSuccessNotification(FileReferenceOne, BatchIdOne, FileTransmissionSuccessOutcome)
  private val failureCallbackPayload: FileTransmissionNotification =
    FileTransmissionSuccessNotification(FileReferenceOne, BatchIdOne, FileTransmissionFailureOutcome)


  "FileUploadNotificationService" should {
    "send SUCCESS notification to the customs notification service" in new SetUp {
      when(mockNotificationConnector.send(any[FileUploadCustomsNotification])).thenReturn(Future.successful(()))
      when(mockFileUploadMetadataRepo.fetch(fileRefEq(FileReferenceOne))(any[HasConversationId])).thenReturn(Future.successful(Some(FileMetadataWithFileOne)))

      await(service.sendMessage(successCallbackPayload, successCallbackPayload.fileReference, subscriptionFieldsId)(toXml))

      verifyNotificationConnectorCalledWithXml(expectedSuccessXml)
    }

    "send FAILURE notification to the customs notification service" in new SetUp {
      when(mockNotificationConnector.send(any[FileUploadCustomsNotification])).thenReturn(Future.successful(()))
      when(mockFileUploadMetadataRepo.fetch(fileRefEq(FileReferenceOne))(any[HasConversationId])).thenReturn(Future.successful(None))

      await(service.sendMessage(failureCallbackPayload, successCallbackPayload.fileReference, subscriptionFieldsId))

      verifyNotificationConnectorCalledWithXml(expectedFailureXml)
    }
  }

}
