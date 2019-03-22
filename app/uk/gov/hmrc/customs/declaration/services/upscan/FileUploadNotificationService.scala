/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.services.upscan

import java.util.UUID

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.upscan.FileUploadCustomsNotificationConnector
import uk.gov.hmrc.customs.declaration.model.SubscriptionFieldsId
import uk.gov.hmrc.customs.declaration.model.upscan.FileReference

import scala.concurrent.Future
import scala.xml.NodeSeq

case class FileUploadCustomsNotification(clientSubscriptionId: SubscriptionFieldsId, conversationId: UUID, payload: NodeSeq)

trait CallbackToXmlNotification[A] {
  def toXml(callbackResponse: A): NodeSeq
}

/**
  Notification sending service
*/
@Singleton
class FileUploadNotificationService @Inject()(notificationConnector: FileUploadCustomsNotificationConnector,
                                              logger: CdsLogger) {

  def sendMessage[T](callbackResponse: T, fileReference: FileReference, clientSubscriptionId: SubscriptionFieldsId)(implicit callbackToXml: CallbackToXmlNotification[T]): Future[Unit] = {

    val notification = FileUploadCustomsNotification(
      clientSubscriptionId, fileReference.value, callbackToXml.toXml(callbackResponse)
    )
    notificationConnector.send(notification)
  }

}
