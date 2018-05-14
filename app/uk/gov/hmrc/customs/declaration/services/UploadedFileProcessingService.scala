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
import uk.gov.hmrc.customs.declaration.connectors.CustomsNotificationServiceConnector
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
  def apply(details: UploadedFileDetails)(implicit successXML: NodeSeq, failedXML: (UploadedFileDetails => NodeSeq)): CustomsNotification = {
    details.upscanNotification.fileStatus match {
      case FileStatus.READY => CustomsNotification(details.clientSubscriptionId, details.upscanNotification.reference, successXML)
      case FileStatus.FAILED => CustomsNotification(details.clientSubscriptionId, details.upscanNotification.reference, failedXML(details))
    }
  }
}


@Singleton
class UploadedFileProcessingService @Inject()(notificationConnector: CustomsNotificationServiceConnector) {

  def sendMessage(details: UploadedFileDetails): Future[Unit] =
    notificationConnector.send(CustomsNotification(details))


  private implicit val uploadedFileScanSuccessXML =
    <root>
      <fileStatus>SUCCESS</fileStatus>
      <details>File successfully received</details>
    </root>

  private implicit def uploadedFileScanFailedXML: UploadedFileDetails => NodeSeq = details =>
    <root>
      <fileStatus>FAILED</fileStatus>
      <details>
        {details.upscanNotification.details.get}
      </details>
    </root>
}
