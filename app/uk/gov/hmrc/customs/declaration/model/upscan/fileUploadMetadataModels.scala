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

package uk.gov.hmrc.customs.declaration.model.upscan

import play.api.libs.json._
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.net.URL
import java.time.Instant
import java.util.UUID
import scala.util.Try

object HttpUrlFormat extends Format[URL] {

  override def reads(json: JsValue): JsResult[URL] = json match {
    case JsString(s) =>
      parseUrl(s).map(JsSuccess(_)).getOrElse(JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.url")))))
    case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.url"))))
  }

  private def parseUrl(s: String): Option[URL] = Try(new URL(s)).toOption

  override def writes(o: URL): JsValue = JsString(o.toString)
}

case class BatchId(value: UUID) extends AnyVal{
  override def toString: String = value.toString
}
object BatchId {
  given writer: Writes[BatchId] = Writes[BatchId] { x => JsString(x.value.toString) }
  given reader: Reads[BatchId] = Reads.of[UUID].map(new BatchId(_))
}

case class DocumentType(value: String) extends AnyVal{
  override def toString: String = value.toString
}
object DocumentType {
  given writer: Writes[DocumentType] = Writes[DocumentType] { x => JsString(x.value) }
  given reader: Reads[DocumentType] = Reads.of[String].map(new DocumentType(_))
}

case class FileReference(value: UUID) extends AnyVal{
  override def toString: String = value.toString
}
object FileReference {
  given writer: Writes[FileReference] = Writes[FileReference] { x => JsString(x.value.toString) }
  given reader: Reads[FileReference] = Reads.of[UUID].map(new FileReference(_))
}

case class CallbackFields(name: String, mimeType: String, checksum: String, uploadTimestamp: Instant, outboundLocation: URL)

object CallbackFields {
  given dateWriter: Writes[Instant] = Writes[Instant] { x =>
    JsString(x.toString)
  }

  given dateReader: Reads[Instant] = Reads[Instant] {
    case JsString(value) =>
      Try(Instant.parse(value)).map(JsSuccess(_)).getOrElse(JsError("Invalid date format for Instant"))
    case _ =>
      JsError("Expected JsString for Instant")
  }

  given urlFormat: HttpUrlFormat.type = HttpUrlFormat

  given format: OFormat[CallbackFields] = Json.format[CallbackFields]
}

case class BatchFile(
  reference: FileReference, // can be used as UNIQUE KEY, upscan-initiate
  maybeCallbackFields: Option[CallbackFields], // upscan-notify
  inboundLocation: URL, // upscan-initiate
  sequenceNumber: FileSequenceNo, // derived from user request
  size: Int, // assumption - it appears to be mandatory but is ignored
  documentType: Option[DocumentType] // user request
)

object BatchFile {
  given urlFormat: HttpUrlFormat.type = HttpUrlFormat
  given format: OFormat[BatchFile] = Json.format[BatchFile]
}

case class FileUploadMetadata(
  declarationId: DeclarationId,
  eori: Eori,
  csId: SubscriptionFieldsId,
  batchId: BatchId,
  fileCount: Int,
  createdAt: Instant,
  files: Seq[BatchFile],
  deferred: Boolean = false,
  completedAt: Option[Instant] = None,
  transmittedAt: Option[Instant] = None
)

object FileUploadMetadata {
  given dateTimeJF: Format[Instant] = MongoJavatimeFormats.instantFormat
  given format: OFormat[FileUploadMetadata] =
    Json.using[Json.WithDefaultValues].format[FileUploadMetadata]
}
