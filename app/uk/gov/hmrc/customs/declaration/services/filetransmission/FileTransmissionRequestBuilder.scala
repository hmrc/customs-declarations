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

package uk.gov.hmrc.customs.declaration.services.filetransmission

import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.filetransmission.*
import uk.gov.hmrc.customs.declaration.model.upscan.{BatchFile, CallbackFields, FileUploadMetadata}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService

import java.net.URL
import javax.inject.{Inject, Singleton}

@Singleton
class FileTransmissionRequestBuilder @Inject()(config: DeclarationsConfigService) {

  def build(md: FileUploadMetadata, bf: BatchFile, cb: CallbackFields, sequenceNumber: FileSequenceNo, fileCount: Int): FileTransmission =
    FileTransmission(
      FileTransmissionBatch(md.batchId, fileCount),
      new URL(s"${config.fileUploadConfig.fileTransmissionCallbackUrl}/file-transmission-notify/clientSubscriptionId/${md.csId}"),
      FileTransmissionFile(
        bf.reference,
        cb.name,
        cb.mimeType,
        cb.checksum,
        cb.outboundLocation,
        sequenceNumber,
        uploadTimestamp = cb.uploadTimestamp),
      FileTransmissionInterface("DEC64", "1.0.0"), //TODO should this come from config instead?
      properties(md, bf))

  private def properties(md: FileUploadMetadata, bf: BatchFile): Seq[FileTransmissionProperty] = {
    val base = Seq("DeclarationId" -> md.declarationId.toString, "Eori" -> md.eori.toString)
      .map(t => FileTransmissionProperty(name = t._1, value = t._2))
    bf.documentType.fold(base)(dt => base :+ FileTransmissionProperty("DocumentType", dt.toString))
  }
}
