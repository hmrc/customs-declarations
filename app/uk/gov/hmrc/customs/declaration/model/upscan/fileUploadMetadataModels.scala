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

package uk.gov.hmrc.customs.declaration.model.upscan

import java.net.URL
import java.time.Instant
import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

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
  implicit val writer = Writes[BatchId] { x => JsString(x.value.toString) }
  implicit val reader = Reads.of[UUID].map(new BatchId(_))
}

case class DocumentType(value: String) extends AnyVal{
  override def toString: String = value.toString
}
object DocumentType {
  implicit val writer = Writes[DocumentType] { x => JsString(x.value) }
  implicit val reader = Reads.of[String].map(new DocumentType(_))
}

case class FileReference(value: UUID) extends AnyVal{
  override def toString: String = value.toString
}
object FileReference {
  implicit val writer = Writes[FileReference] { x => JsString(x.value.toString) }
  implicit val reader = Reads.of[UUID].map(new FileReference(_))
}

case class CallbackFields(name: String, mimeType: String, checksum: String, uploadTimestamp: Instant, outboundLocation: URL)
object CallbackFields {
  implicit val urlFormat = HttpUrlFormat
  implicit val format = Json.format[CallbackFields]
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
  implicit val urlFormat = HttpUrlFormat
  implicit val format = Json.format[BatchFile]
}

case class FileUploadMetadata(
  declarationId: DeclarationId,
  eori: Eori,
  csId: SubscriptionFieldsId,
  batchId: BatchId,
  fileCount: Int,
  createdAt: DateTime,
  files: Seq[BatchFile]
)
object FileUploadMetadata {
  implicit val dateTimeJF = ReactiveMongoFormats.dateTimeFormats
  implicit val format = Json.format[FileUploadMetadata]
  implicit val fileUploadMetadataJF = ReactiveMongoFormats.mongoEntity(Json.format[FileUploadMetadata])
}
