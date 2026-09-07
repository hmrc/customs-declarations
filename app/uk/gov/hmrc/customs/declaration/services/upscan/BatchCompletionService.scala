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
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.upscan.*
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.filetransmission.FileTransmissionRequestBuilder
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BatchCompletionService @Inject()(repo: FileUploadMetadataRepo,
                                       connector: FileTransmissionConnector,
                                       builder: FileTransmissionRequestBuilder)(using ExecutionContext) {

  //TODO: consider different scenarios transmission in progress, pending, invalid, already completed, batch not found, etc
  def complete(batchId: BatchId, csId: SubscriptionFieldsId, eori: Option[Eori], references: Seq[FileReference])
              (using HeaderCarrier, HasConversationId): Future[Unit] = {
    repo.claimForCompletion(batchId, csId, Instant.now()).flatMap {
      case None =>
        ??? //TODO
      case Some(metadata) =>
        val fileCount = metadata.files.size

        val requests = metadata.files.zipWithIndex.map { case (bf, index) =>
          builder.build(metadata, bf, bf.maybeCallbackFields.get, FileSequenceNo(index + 1), fileCount)
        }

        //for each request send a message to file transmission
        //this accomplishes same as previous cb FileUploadUpscanNotificationBusinessService.persistAndCallFileTransmission
        //we do them all at once at the end whenever cds-file-upload-frontend sends a post to /file-upload/complete
        for {
          _ <- requests.traverse_(connector.send)
          _ <- repo.markTransmitted(metadata.batchId, metadata.csId, Instant.now()) //TODO use DateTimeService
        } yield ()
    }
  }
}
