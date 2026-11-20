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

package uk.gov.hmrc.customs.declaration.services.upscan

import uk.gov.hmrc.customs.declaration.connectors.filetransmission.FileTransmissionConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.filetransmission.*
import uk.gov.hmrc.customs.declaration.model.transmission.{Batch, File, Interface, Property, TransmissionRequest, TransmissionRequestEnvelope}
import uk.gov.hmrc.customs.declaration.model.upscan.*
import uk.gov.hmrc.customs.declaration.repo.{FileUploadMetadataRepo, TransmissionRequestRepo}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HeaderCarrier

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadUpscanNotificationBusinessService @Inject()(repo: FileUploadMetadataRepo,
                                                            connector: FileTransmissionConnector,
                                                            transmissionRequestRepo: TransmissionRequestRepo,
                                                            config: DeclarationsConfigService,
                                                            logger: DeclarationsLogger)
                                                           (implicit ec: ExecutionContext) {

  def persistAndCallFileTransmission(csId: SubscriptionFieldsId, ready: UploadedReadyCallbackBody)(implicit r: HasConversationId, hc: HeaderCarrier): Future[Unit] = {
    repo.update(
      csId,
      ready.reference,
      CallbackFields(ready.uploadDetails.fileName, ready.uploadDetails.fileMimeType, ready.uploadDetails.checksum, ready.uploadDetails.uploadTimestamp, ready.downloadUrl)
    ).flatMap {
      case None =>
        val errorMsg = s"database error - can't find record with file reference [${ready.reference}]"
        logger.error(errorMsg)
        Future.failed(new IllegalStateException(errorMsg))
      case Some(metadata) =>
        logger.debug(s"updated fileUploadMetadata: $metadata")
        if (config.fileTransmissionEnabled) {
          callFileTransmission(ready, metadata)
        } else {
          queueTransmissionRequest(ready, metadata)
        }
    }
  }

  private def fileNotFoundError(reference: FileReference)(implicit r: HasConversationId): Future[Nothing] = {
    val errorMsg = s"database error - can't find file with file reference [$reference]"
    logger.error(errorMsg)
    Future.failed(new IllegalStateException(errorMsg))
  }

  private def callFileTransmission(ready: UploadedReadyCallbackBody, md: FileUploadMetadata)(implicit r: HasConversationId, hc: HeaderCarrier): Future[Unit] =
    maybeFileTransmission(ready, md) match {
      case None => fileNotFoundError(ready.reference)
      case Some(fileTransmission) =>
        connector.send(fileTransmission).map { _ =>
          logger.info(s"successfully called file transmission service with batchId [${fileTransmission.batch.id.toString}], callbackUrl [${fileTransmission.callbackUrl.toString}] and fileReference [${fileTransmission.file.reference.toString}]")
          ()
        }
    }

  private def queueTransmissionRequest(ready: UploadedReadyCallbackBody, md: FileUploadMetadata)(implicit r: HasConversationId): Future[Unit] =
    maybeTransmissionRequestEnvelope(ready, md) match {
      case None => fileNotFoundError(ready.reference)
      case Some(envelope) =>
        transmissionRequestRepo.pushNewIfAbsent(envelope).map { queued =>
          if (queued)
            logger.info(s"successfully queued transmission request with batchId [${envelope.request.batch.id}], callbackUrl [${envelope.request.callbackUrl}] and fileReference [${envelope.request.file.reference}]")
          else
            logger.info(s"duplicate READY callback, transmission request already queued for fileReference [${envelope.request.file.reference}]")
          ()
        }
    }

  private def maybeFileTransmission(ready: UploadedReadyCallbackBody, md: FileUploadMetadata): Option[FileTransmission] =
    for {
      (bf, cbFields) <- maybeBatchFileWithCallbackFields(ready.reference, md)
    } yield
      FileTransmission(
        FileTransmissionBatch(md.batchId, md.fileCount),
        new URL(s"${config.fileUploadConfig.fileTransmissionCallbackUrl}/file-transmission-notify/clientSubscriptionId/${md.csId}"),
        FileTransmissionFile(bf.reference, cbFields.name, cbFields.mimeType, cbFields.checksum, cbFields.outboundLocation, bf.sequenceNumber, uploadTimestamp = cbFields.uploadTimestamp),
        FileTransmissionInterface("DEC64", "1.0.0"),
        extractFileTransmissionProperties(md, bf)
      )

  private def maybeTransmissionRequestEnvelope(ready: UploadedReadyCallbackBody, md: FileUploadMetadata): Option[TransmissionRequestEnvelope] =
    for {
      (bf, cbFields) <- maybeBatchFileWithCallbackFields(ready.reference, md)
    } yield
      TransmissionRequestEnvelope(
        request = TransmissionRequest(
          batch = Batch(md.batchId.toString, md.fileCount),
          interface = Interface("DEC64", "1.0.0"),
          file = File(bf.reference.toString, cbFields.outboundLocation, cbFields.name, cbFields.mimeType, cbFields.checksum, bf.sequenceNumber.value, bf.size, cbFields.uploadTimestamp),
          properties = extractTransmissionProperties(md, bf),
          callbackUrl = new URL(s"${config.fileUploadConfig.fileTransmissionCallbackUrl}/file-transmission-notify/clientSubscriptionId/${md.csId}"),
          deliveryWindowDuration = None
        ),
        serviceName = "customs-declarations",
        csId = md.csId
      )

  private def maybeBatchFileWithCallbackFields(fileReference: FileReference, metadata: FileUploadMetadata): Option[(BatchFile, CallbackFields)] =
    for {
      batchFile <- metadata.files.find(_.reference == fileReference)
      cbFields  <- batchFile.maybeCallbackFields
    } yield (batchFile, cbFields)

  private def extractFileTransmissionProperties(md: FileUploadMetadata, bf: BatchFile): Seq[FileTransmissionProperty] = {
    val props = Seq("DeclarationId" -> md.declarationId.toString, "Eori" -> md.eori.toString)
      .map(t => FileTransmissionProperty(name = t._1, value = t._2))
    if (bf.documentType.isDefined) props :+ FileTransmissionProperty("DocumentType", bf.documentType.get.toString) else props
  }

  private def extractTransmissionProperties(md: FileUploadMetadata, bf: BatchFile): Seq[Property] = {
    val props = Seq("DeclarationId" -> md.declarationId.toString, "Eori" -> md.eori.toString)
      .map(t => Property(name = t._1, value = t._2))
    if (bf.documentType.isDefined) props :+ Property("DocumentType", bf.documentType.get.toString) else props
  }

}
