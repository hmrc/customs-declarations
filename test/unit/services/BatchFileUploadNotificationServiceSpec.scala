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

package unit.services

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito._
import org.scalatest.Assertion
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Reads
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.BatchFileUploadCustomsNotificationConnector
import uk.gov.hmrc.customs.declaration.model.{BatchId, FileReference}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.play.test.UnitSpec
import unit.services.ExampleFileTransmissionStatus.ExampleFileTransmissionStatus
import util.ApiSubscriptionFieldsTestData
import util.TestData._

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.{Node, NodeSeq, Utility, XML}

object ExampleFileTransmissionStatus extends Enumeration {
  type ExampleFileTransmissionStatus = Value
  val SUCCESS, FAILURE = Value

  implicit val fileTransmissionStatusReads = Reads.enumNameReads(ExampleFileTransmissionStatus)
}

case class ExampleFileTransmissionNotification(fileReference: FileReference,
                                               batchId: BatchId,
                                               fileTransmissionStatus: ExampleFileTransmissionStatus,
                                               errorDetails: Option[String])

class BatchFileUploadNotificationServiceSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    val mockNotificationConnector = mock[BatchFileUploadCustomsNotificationConnector]
    val mockDeclarationsLogger = mock[CdsLogger]
    val service = new BatchFileUploadNotificationService(mockNotificationConnector, mockDeclarationsLogger)
    val expectedSuccessXml =
      <Root>
        <FileReference>{FileReferenceOne}</FileReference>
        <BatchId>{BatchIdOne}</BatchId>
        <Outcome>SUCCESS</Outcome>
        <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
      </Root>
    val expectedFailureXml =
      <Root>
        <FileReference>{FileReferenceOne}</FileReference>
        <BatchId>{BatchIdOne}</BatchId>
        <Outcome>FAILURE</Outcome>
        <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
      </Root>

    def verifyNotificationConnectorCalledWithXml(xml: NodeSeq): Assertion = {
      val captor: ArgumentCaptor[BatchFileUploadCustomsNotification] = ArgumentCaptor.forClass(classOf[BatchFileUploadCustomsNotification])
      verify(mockNotificationConnector).send(captor.capture())
      val actual: BatchFileUploadCustomsNotification = captor.getValue
      actual.clientSubscriptionId shouldBe ApiSubscriptionFieldsTestData.subscriptionFieldsId
      actual.conversationId shouldBe FileReferenceOne.value
      string2xml(actual.payload.toString) shouldBe string2xml(xml.toString)
    }
  }

  // Example  CallbackToXmlNotification implementation for file transmission response
  class FileTransmissionToCallbackToXmlNotification extends CallbackToXmlNotification[ExampleFileTransmissionNotification] {
    override def toXml(callbackResponse: ExampleFileTransmissionNotification): NodeSeq = {
      val (status, details) =
        if (callbackResponse.fileTransmissionStatus == ExampleFileTransmissionStatus.SUCCESS) {
          ("SUCCESS", "Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.")
        } else {
          ("FAILURE", "A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.")
        }
      <Root>
        <FileReference>{callbackResponse.fileReference.toString}</FileReference>
        <BatchId>{callbackResponse.batchId.toString}</BatchId>
        <Outcome>{status}</Outcome>
        <Details>{details}</Details>
      </Root>
    }
  }

  // endpoint specific callback payload
  private val successCallbackPayload = ExampleFileTransmissionNotification(FileReferenceOne, BatchIdOne, ExampleFileTransmissionStatus.SUCCESS, None)
  private val failureCallbackPayload = ExampleFileTransmissionNotification(FileReferenceOne, BatchIdOne, ExampleFileTransmissionStatus.FAILURE, None)

  private implicit val toXml = new FileTransmissionToCallbackToXmlNotification()

  "BatchFileUploadNotificationService" should {
    "send SUCCESS notification to the customs notification service" in new SetUp {
      when(mockNotificationConnector.send(any[BatchFileUploadCustomsNotification])).thenReturn(Future.successful(()))

      await(service.sendMessage(successCallbackPayload, successCallbackPayload.fileReference, ApiSubscriptionFieldsTestData.subscriptionFieldsId))

      verifyNotificationConnectorCalledWithXml(expectedSuccessXml)
    }

    "send FAILURE notification to the customs notification service" in new SetUp {
      when(mockNotificationConnector.send(any[BatchFileUploadCustomsNotification])).thenReturn(Future.successful(()))

      await(service.sendMessage(failureCallbackPayload, successCallbackPayload.fileReference, ApiSubscriptionFieldsTestData.subscriptionFieldsId))

      verifyNotificationConnectorCalledWithXml(expectedFailureXml)
    }
  }

  private def string2xml(s: String): Node = {
    val xml = try {
      XML.loadString(s)
    } catch {
      case NonFatal(thr) => fail("Not an xml: " + s, thr)
    }
    Utility.trim(xml)
  }

}
