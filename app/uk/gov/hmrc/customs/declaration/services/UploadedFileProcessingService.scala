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

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.declaration.connectors.CustomsNotificationConnector
import uk.gov.hmrc.customs.declaration.controllers.{FileStatus, UpscanNotification}

import scala.concurrent.Future
import scala.xml.NodeSeq


case class UploadedFileDetails(
                                decId: String,
                                eori: String,
                                docType: String,
                                clientSubscriptionId: String,
                                upscanNotification: UpscanNotification)

case class CustomsNotification(clientSubscriptionId: String, conversationId: UUID, payload: NodeSeq)

object CustomsNotification {
  def apply(details: UploadedFileDetails)(successXML: (UploadedFileDetails => NodeSeq), failedXML: (UploadedFileDetails => NodeSeq)): CustomsNotification = {
    details.upscanNotification.fileStatus match {
      case FileStatus.READY => CustomsNotification(details.clientSubscriptionId, details.upscanNotification.reference, successXML(details))
      case FileStatus.FAILED => CustomsNotification(details.clientSubscriptionId, details.upscanNotification.reference, failedXML(details))
    }
  }
}


@Singleton
class UploadedFileProcessingService @Inject()(notificationConnector: CustomsNotificationConnector) {

  def sendMessage(details: UploadedFileDetails): Future[Unit] =
    notificationConnector.send(CustomsNotification(details)(uploadedFileScanSuccessXML, uploadedFileScanFailedXML))

  private def uploadedFileScanSuccessXML: UploadedFileDetails => NodeSeq = details =>
    <root>
      <fileStatus>SUCCESS</fileStatus>
      <uploadDetails>
        <uploadTimestamp>{details.upscanNotification.uploadDetails.get.uploadTimestamp.get}</uploadTimestamp>
        <checksum>{details.upscanNotification.uploadDetails.get.checksum.get}</checksum>
      </uploadDetails>
    </root>

  private def uploadedFileScanFailedXML: UploadedFileDetails => NodeSeq = details =>
    <root>
      <fileStatus>FAILED</fileStatus>
      <failureDetails>
        <failureReason>
          {details.upscanNotification.failureDetails.get.failureReason}
        </failureReason>
        <message>
          {details.upscanNotification.failureDetails.get.message}
        </message>
      </failureDetails>
    </root>
}
