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

import java.util.UUID

import org.mockito.Mockito.{reset, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.connectors.CustomsNotificationConnector
import uk.gov.hmrc.customs.declaration.controllers.FileStatus._
import uk.gov.hmrc.customs.declaration.controllers.UpscanNotification
import uk.gov.hmrc.customs.declaration.services.{CustomsNotification, UploadedFileDetails, UploadedFileProcessingService}

import scala.concurrent.Future
import scala.xml.Utility.trim


class UploadedFileProcessingServiceSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  private val customsNotificationConnector = mock[CustomsNotificationConnector]

  private val service = new UploadedFileProcessingService(customsNotificationConnector)

  private val readyStatusFD = UploadedFileDetails("decId", "eori", "docType", "clientSubscriptionId",
    UpscanNotification(UUID.randomUUID(), READY, None, Some("https://some-url")))

  private val failedStatusFD = UploadedFileDetails("decId", "eori", "docType", "clientSubscriptionId",
    UpscanNotification(UUID.randomUUID(), FAILED, Some("The file has a virus"), Some("https://some-url")))

  override def beforeEach(): Unit = {
    reset(customsNotificationConnector)
    when(customsNotificationConnector.send(ArgumentMatchers.any[CustomsNotification]))
    .thenReturn(Future.successful(()))
  }

  "UploadedFileProcessingService" should {

    "call the Customs Notification connector with correct details when FileStatus is READY" in {
      await(service.sendMessage(readyStatusFD))

      val ac: ArgumentCaptor[CustomsNotification] = ArgumentCaptor.forClass(classOf[CustomsNotification])
      verify(customsNotificationConnector).send(ac.capture())

      val notification = ac.getValue

      notification.clientSubscriptionId must be(readyStatusFD.clientSubscriptionId)
      notification.conversationId must be(readyStatusFD.upscanNotification.reference)

      val expectedXML = <root>
        <fileStatus>SUCCESS</fileStatus>
        <details>File successfully received</details>
      </root>

      trim(expectedXML) must be (trim(notification.payload(0)))
    }

    "call the Customs Notification connector with correct details when FileStatus is FAILED" in {
      await(service.sendMessage(failedStatusFD))

      val ac: ArgumentCaptor[CustomsNotification] = ArgumentCaptor.forClass(classOf[CustomsNotification])
      verify(customsNotificationConnector).send(ac.capture())

      val notification = ac.getValue

      notification.clientSubscriptionId must be(failedStatusFD.clientSubscriptionId)
      notification.conversationId must be(failedStatusFD.upscanNotification.reference)

      val expectedXML = <root>
        <fileStatus>FAILED</fileStatus>
        <details>{failedStatusFD.upscanNotification.details.get}</details>
      </root>

      trim(expectedXML) must be (trim(notification.payload(0)))
    }
  }


}
