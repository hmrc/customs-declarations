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

package unit.services.upscan

import java.net.URL
import java.util.UUID
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.connectors.filetransmission.FileTransmissionConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.filetransmission._
import uk.gov.hmrc.customs.declaration.model.upscan._
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.customs.declaration.services.upscan.FileUploadUpscanNotificationBusinessService
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.TestData._

import scala.concurrent.Future

class FileUploadUpscanNotificationBusinessServiceSpec extends AnyWordSpecLike with MockitoSugar with Matchers{

  private val outboundUrl = new URL("http://remotehost/outbound-bucket/123")
  private val uploadDetails = UploadedDetails("test.pdf", "application/pdf", InitiateDate, "1a2b3c4d5e")
  private val readyCallbackBody = UploadedReadyCallbackBody(FileReferenceOne, outboundUrl, ReadyFileStatus, uploadDetails)
  private val callbackFields = CallbackFields(uploadDetails.fileName, uploadDetails.fileMimeType, uploadDetails.checksum, InitiateDate, outboundUrl)

  private val md = FileMetadataWithFilesOneAndThree
  private val mdFileOne = FileMetadataWithFilesOneAndThree.files.head
  private val mdFileOneCallback = mdFileOne.maybeCallbackFields.get
  private val fileTransmissionBatchOne = FileTransmissionBatch(md.batchId, md.fileCount)
  private val fileTransmissionCallbackUrl = "http://file_transmission_callback_url"
  private val fileTransmissionServiceURL = "http://file_transmission_service_url"
  private val fileTransmissionLocation = mdFileOne.maybeCallbackFields.get.outboundLocation
  private val fileTransmissionFileOne = FileTransmissionFile(mdFileOne.reference, mdFileOneCallback.name, mdFileOneCallback.mimeType, mdFileOneCallback.checksum, location = fileTransmissionLocation, mdFileOne.sequenceNumber, uploadTimestamp = InitiateDate)
  private val fileTransmissionInterfaceOne = FileTransmissionInterface("DEC64", "1.0.0")
  private val fileTransmissionProperties = Seq(
    FileTransmissionProperty("DeclarationId", md.declarationId.toString),
    FileTransmissionProperty("Eori", md.eori.toString),
    FileTransmissionProperty("DocumentType", mdFileOne.documentType.get.toString)
  )
  private val fileTransmissionRequest = FileTransmission(fileTransmissionBatchOne, new URL(s"$fileTransmissionCallbackUrl/file-transmission-notify/clientSubscriptionId/$clientSubscriptionIdString"), fileTransmissionFileOne, fileTransmissionInterfaceOne, fileTransmissionProperties)
  private implicit val ec = Helpers.stubControllerComponents().executionContext
  private implicit val implicitHasConversationId: HasConversationId = new HasConversationId {
    override val conversationId: ConversationId = ConversationId(FileReferenceOne.value)
  }
  private val fileGroupSizeMaximum = 5
  private val fileUploadConfig = FileUploadConfig("UPSCAN_INITIATE_V1_URL", "UPSCAN_INITIATE_V2_URL", TenMb, "UPSCAN_URL_IGNORED", fileGroupSizeMaximum, fileTransmissionCallbackUrl, fileTransmissionServiceURL, 600)

  trait SetUp {
    val mockRepo = mock[FileUploadMetadataRepo]
    val mockConnector = mock[FileTransmissionConnector]
    val mockConfig = mock[DeclarationsConfigService]
    val mockLogger = mock[DeclarationsLogger]
    val service = new FileUploadUpscanNotificationBusinessService(mockRepo, mockConnector, mockConfig, mockLogger)

    when(mockConfig.fileUploadConfig).thenReturn(fileUploadConfig)
  }

  "FileUploadUpscanNotificationBusinessService" should {
    "update metadata and call file transmission service" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.successful(Some(FileMetadataWithFilesOneAndThree)))
      when(mockConnector.send(any[FileTransmission])(any[HasConversationId])).thenReturn(Future.successful(()))

      val actual: Unit = await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody))

      actual shouldBe (())
      verify(mockRepo).update(
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId],
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq(callbackFields))(any[HasConversationId]
      )
      verify(mockConnector).send(ameq(fileTransmissionRequest))(any[HasConversationId])
    }

    "return failed future when no metadata record found for file reference" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.successful(None))

      val error = intercept[IllegalStateException](await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody)))

      error.getMessage shouldBe s"database error - can't find record with file reference ${FileReferenceOne.value.toString}"
      verify(mockRepo).update(
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId],
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq(callbackFields))(any[HasConversationId]
      )
      verifyZeroInteractions(mockConnector)
    }

    "return failed future when file reference not found in returned metadata" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.successful(Some(FileMetadataWithFileTwo)))

      val error = intercept[IllegalStateException](await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody)))

      error.getMessage shouldBe s"database error - can't find file with file reference ${FileReferenceOne.value.toString}"
      verify(mockRepo).update(
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId],
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq(callbackFields))(any[HasConversationId]
      )
      verifyZeroInteractions(mockConnector)
    }

    "propagate exception encountered in repo" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.failed(emulatedServiceFailure))

      val error = intercept[EmulatedServiceFailure](await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody)))

      error shouldBe emulatedServiceFailure
      verify(mockRepo).update(
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId],
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq(callbackFields))(any[HasConversationId]
      )
      verifyZeroInteractions(mockConnector)
    }

    "propagate exception encountered in connector" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.successful(Some(FileMetadataWithFilesOneAndThree)))
      when(mockConnector.send(any[FileTransmission])(any[HasConversationId])).thenReturn(Future.failed(emulatedServiceFailure))

      val error = intercept[EmulatedServiceFailure](await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody)))

      error shouldBe emulatedServiceFailure
    }
  }
}
