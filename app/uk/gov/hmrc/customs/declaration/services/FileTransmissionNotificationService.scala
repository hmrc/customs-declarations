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
//TODO to be deleted
package uk.gov.hmrc.customs.declaration.services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.BatchFileNotificationConnector
import uk.gov.hmrc.customs.declaration.model.{FileTransmissionFailureOutcome, FileTransmissionNotification, FileTransmissionOutcome, FileTransmissionSuccessOutcome}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

case class FileTransmissionCustomsNotification(clientSubscriptionId: String, conversationId: UUID, payload: NodeSeq)
object FileTransmissionCustomsNotification {
  def apply(fileTransmissionNotification: FileTransmissionNotification,
            clientSubscriptionId: String)
           (successXML: (FileTransmissionNotification => NodeSeq),
            failedXML: (FileTransmissionNotification => NodeSeq)): FileTransmissionCustomsNotification = {
    fileTransmissionNotification.outcome match {
      case FileTransmissionSuccessOutcome => FileTransmissionCustomsNotification(clientSubscriptionId,
        fileTransmissionNotification.reference.value, successXML(fileTransmissionNotification))
      case FileTransmissionFailureOutcome => FileTransmissionCustomsNotification(clientSubscriptionId,
        fileTransmissionNotification.reference.value, failedXML(fileTransmissionNotification))
    }
  }
}

@Singleton
class FileTransmissionNotificationService @Inject() (notificationConnector: BatchFileNotificationConnector,
                                                     cdsLogger: CdsLogger) {

  def sendMessage(fileTransmissionNotification: FileTransmissionNotification, clientSubscriptionId: String): Future[Either[Result, Unit]] = {

    notificationConnector.send(FileTransmissionCustomsNotification(fileTransmissionNotification, clientSubscriptionId)
      (uploadedFileTransmissionSuccessXML, uploadedFileTransmissionFailureXML))
    Future.successful(Right(()))
  }.recover {
    case e: Throwable =>
      cdsLogger.error(s"[conversationId=${fileTransmissionNotification.reference.toString}][clientSubscriptionId=$clientSubscriptionId] file transmission notification service request to Customs Notification failed.", e)
      Left(ErrorResponse.ErrorInternalServerError.JsonResult)
  }

  private def uploadedFileTransmissionSuccessXML: FileTransmissionNotification => NodeSeq = fileTransmissionNotification =>
    <Root>
      <FileReference>{fileTransmissionNotification.reference.toString}</FileReference>
      <BatchId>{fileTransmissionNotification.batchId.toString}</BatchId>
      <Outcome>SUCCESS</Outcome>
      <Details>Thank you for submitting your documents. Typical clearance times are 2 hours for air and 3 hours for maritime declarations. During busy periods wait times may be longer.</Details>
    </Root>

  private def uploadedFileTransmissionFailureXML: FileTransmissionNotification => NodeSeq = fileTransmissionNotification =>
    <Root>
      <FileReference>{fileTransmissionNotification.reference.toString}</FileReference>
      <BatchId>{fileTransmissionNotification.batchId.toString}</BatchId>
      <Outcome>FAILURE</Outcome>
      <Details>A system error has prevented your document from being accepted. Please follow the guidance on www.gov.uk and submit your documents by an alternative method.</Details>
    </Root>

}
