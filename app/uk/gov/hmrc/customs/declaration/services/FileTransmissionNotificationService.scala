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

package uk.gov.hmrc.customs.declaration.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.FileTransmissionCustomsNotificationConnector
import uk.gov.hmrc.customs.declaration.controllers.{FileTransmissionNotification, FileTransmissionStatus}

import scala.concurrent.Future
import scala.xml.NodeSeq

case class FileTransmissionCustomsNotification(clientSubscriptionId: String, conversationId: UUID, payload: NodeSeq)
object FileTransmissionCustomsNotification {
  def apply(fileTransmissionNotification: FileTransmissionNotification,
            clientSubscriptionId: String)
           (successXML: (FileTransmissionNotification => NodeSeq),
            failedXML: (FileTransmissionNotification => NodeSeq)): FileTransmissionCustomsNotification = {
    fileTransmissionNotification.fileTransmissionStatus match {
      case FileTransmissionStatus.SUCCESS => FileTransmissionCustomsNotification(clientSubscriptionId,
        fileTransmissionNotification.fileReference.value, successXML(fileTransmissionNotification))
      case FileTransmissionStatus.FAILURE => FileTransmissionCustomsNotification(clientSubscriptionId,
        fileTransmissionNotification.fileReference.value, failedXML(fileTransmissionNotification))
    }
  }
}

@Singleton
class FileTransmissionNotificationService @Inject() (notificationConnector: FileTransmissionCustomsNotificationConnector,
                                                     declarationsLogger: CdsLogger) {

  def sendMessage(fileTransmissionNotification: FileTransmissionNotification, clientSubscriptionId: String): Future[Unit] = {

    notificationConnector.send(FileTransmissionCustomsNotification(fileTransmissionNotification, clientSubscriptionId)
      (uploadedFileTransmissionSuccessXML, uploadedFileTransmissionFailureXML))
  }

  private def uploadedFileTransmissionSuccessXML: FileTransmissionNotification => NodeSeq = fileTransmissionNotification =>
    <Root>
      <FileReference>{fileTransmissionNotification.fileReference.toString}</FileReference>
      <BatchId>{fileTransmissionNotification.batchId.toString}</BatchId>
      <Outcome>SUCCESS</Outcome>
      <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
    </Root>

  private def uploadedFileTransmissionFailureXML: FileTransmissionNotification => NodeSeq = fileTransmissionNotification =>
    <Root>
      <FileReference>{fileTransmissionNotification.fileReference.toString}</FileReference>
      <BatchId>{fileTransmissionNotification.batchId.toString}</BatchId>
      <Outcome>FAILURE</Outcome>
      <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
    </Root>

}
