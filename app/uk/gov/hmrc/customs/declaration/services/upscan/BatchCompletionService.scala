/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.implicits.*
import uk.gov.hmrc.customs.declaration.connectors.filetransmission.FileTransmissionConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.upscan.*
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.filetransmission.FileTransmissionRequestBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.customs.declaration.services.DateTimeService

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BatchCompletionService @Inject()(fileUploadMetadataRepo: FileUploadMetadataRepo,
                                       fileTransmissionConnector: FileTransmissionConnector,
                                       logger: DeclarationsLogger,
                                       fileTransmissionRequestBuilder: FileTransmissionRequestBuilder,
                                       dateTimeService: DateTimeService)(using ExecutionContext) {

  def complete(batchId: BatchId, csId: SubscriptionFieldsId, eori: Option[Eori], references: Seq[FileReference])
              (using HeaderCarrier, HasConversationId): Future[CompletionResult] = {

    fileUploadMetadataRepo.fetchByBatchId(batchId, csId).flatMap {
      case None => Future.successful(BatchNotFound)
      case Some(md) if md.completedAt.isDefined =>
        val status = md.transmittedAt.map(_ => AlreadyCompleted).getOrElse(CompletionInProgress)
        Future.successful(status)
      case Some(md) =>
        fileUploadMetadataRepo.claimForCompletion(batchId, csId, Instant.now()).flatMap {
          case None =>
            Future.successful(BatchNotFound)
          case Some(metadata) =>
            val filesInMetadata = metadata.files.groupBy(_.reference).view.mapValues(_.head).toMap
            val contactDetailsFile = metadata.files.find(_.maybeCallbackFields.exists(_.name.startsWith("contact_details_")))
            val filesForCompletion = (contactDetailsFile :: references.map(filesInMetadata.get).toList).flatten //excluding files that were removed by user
            fileUploadMetadataRepo.updateFiles(batchId, csId, filesForCompletion)
            val fileCount = filesForCompletion.size
            val fileTransmissionRequests = filesForCompletion.zipWithIndex.map { case (batchFile, index) =>
              fileTransmissionRequestBuilder.build(metadata, batchFile, batchFile.maybeCallbackFields.get, FileSequenceNo(index + 1), fileCount)
            }
            //For each fileTransmissionRequest, send it to the File Transmission service
            //This accomplishes same as previous cb FileUploadUpscanNotificationBusinessService.persistAndCallFileTransmission
            //We do them all at once at the end, whenever cds-file-upload-frontend sends a POST to /file-upload/complete
            for {
              _ <- fileTransmissionRequests.traverse_(fileTransmissionConnector.send)
              _ <- fileUploadMetadataRepo.markTransmitted(metadata.batchId, metadata.csId, dateTimeService.nowUtc())
            } yield Completed(fileCount)
        }
    }
  }
}
