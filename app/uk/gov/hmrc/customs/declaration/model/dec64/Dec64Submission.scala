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

package uk.gov.hmrc.customs.declaration.model.dec64

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.customs.declaration.model.{FileSequenceNo, SubscriptionFieldsId}
import uk.gov.hmrc.customs.declaration.model.upscan.{BatchId, FileReference, HttpUrlFormat}

import java.net.URL
import java.time.Instant

case class Dec64Property(name: String, value: String)

object Dec64Property {
  implicit val format: OFormat[Dec64Property] = Json.format[Dec64Property]
}

case class Dec64Submission(
  fileReference: FileReference,
  csId: SubscriptionFieldsId,
  batchId: BatchId,
  fileCount: Int,
  sequenceNumber: FileSequenceNo,
  fileName: String,
  mimeType: String,
  checksum: String,
  size: Option[Long],
  uploadTimestamp: Instant,
  upscanDownloadUrl: URL,
  properties: Seq[Dec64Property],
  objectStorePath: Option[String],
  deliveryDeadline: Instant,
  outcome: Option[Dec64Outcome] = None
)

object Dec64Submission {
  implicit val urlFormat: HttpUrlFormat.type = HttpUrlFormat
  implicit val format: OFormat[Dec64Submission] = Json.format[Dec64Submission]
}
