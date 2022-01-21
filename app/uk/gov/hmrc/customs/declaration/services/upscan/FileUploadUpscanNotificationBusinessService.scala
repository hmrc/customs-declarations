/*
 * Copyright 2022 HM Revenue & Customs
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

import java.net.URL

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.declaration.connectors.filetransmission.FileTransmissionConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.filetransmission._
import uk.gov.hmrc.customs.declaration.model.upscan._
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadUpscanNotificationBusinessService @Inject()(repo: FileUploadMetadataRepo,
                                                            connector: FileTransmissionConnector,
                                                            config: DeclarationsConfigService,
                                                            logger: DeclarationsLogger)
                                                           (implicit ec: ExecutionContext) {

  def persistAndCallFileTransmission(csId: SubscriptionFieldsId, ready: UploadedReadyCallbackBody)(implicit r: HasConversationId): Future[Unit] = {
    repo.update(
      csId,
      ready.reference,
      CallbackFields(ready.uploadDetails.fileName, ready.uploadDetails.fileMimeType, ready.uploadDetails.checksum, ready.uploadDetails.uploadTimestamp, ready.downloadUrl)
    ).flatMap{
      case None =>
        val errorMsg = s"database error - can't find record with file reference ${ready.reference}"
        logger.error(errorMsg)
        Future.failed(new IllegalStateException(errorMsg))
      case Some(metadata) =>
        logger.debug(s"updated fileUploadMetadata: $metadata")
        maybeFileTransmission(ready, metadata) match {
          case None =>
            val errorMsg = s"database error - can't find file with file reference ${ready.reference}"
            logger.error(errorMsg)
            Future.failed(new IllegalStateException(errorMsg))
          case Some(fileTransmission) =>
            connector.send(fileTransmission).map { _ =>
              logger.info(s"successfully called file transmission service with batchId ${fileTransmission.batch.id.toString}, callbackUrl ${fileTransmission.callbackUrl.toString} and fileReference ${fileTransmission.file.reference.toString}")
              ()
            }
      }
    }
  }

  private def maybeFileTransmission(ready: UploadedReadyCallbackBody, md: FileUploadMetadata): Option[FileTransmission] = {
    for {
      (bf, ftf) <- maybeFileTransmissionFile(ready.reference, md)
    } yield
      FileTransmission(FileTransmissionBatch(md.batchId, md.fileCount),
        new URL(s"${config.fileUploadConfig.fileTransmissionCallbackUrl}/file-transmission-notify/clientSubscriptionId/${md.csId}"),
        ftf,
        FileTransmissionInterface("DEC64", "1.0.0"),
        extractFileProperties(ready, md, bf)
    )
  }

  private def extractFileProperties(ready: UploadedReadyCallbackBody, md: FileUploadMetadata, bf: BatchFile): Seq[FileTransmissionProperty] = {
    val fileProperties = Seq("DeclarationId" -> md.declarationId.toString, "Eori" -> md.eori.toString)
      .map(t => FileTransmissionProperty(name = t._1, value = t._2))
    if (bf.documentType.isDefined) fileProperties :+ FileTransmissionProperty("DocumentType", bf.documentType.get.toString) else fileProperties
  }

  private def maybeFileTransmissionFile(fileReference: FileReference, metadata: FileUploadMetadata): Option[(BatchFile, FileTransmissionFile)] = {
    for {
      batchFile <- metadata.files.find(bf => bf.reference == fileReference)
      cbFields <- batchFile.maybeCallbackFields
    } yield (
      batchFile,
      FileTransmissionFile(
        batchFile.reference,
        cbFields.name,
        cbFields.mimeType,
        cbFields.checksum,
        cbFields.outboundLocation,
        batchFile.sequenceNumber,
        uploadTimestamp = cbFields.uploadTimestamp
      )
    )
  }

}
