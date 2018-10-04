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

package uk.gov.hmrc.customs.declaration.model

import java.net.URL

import play.api.libs.json.Json

case class FileTransmissionBatch(
  id: BatchId,
  fileCount: Int
)
object FileTransmissionBatch {
  implicit val writes = Json.writes[FileTransmissionBatch]
}

case class FileTransmissionFile(
  reference: FileReference,
  name: String,
  mimeType: String,
  checkSum: String,
  location: URL,
  sequenceNumber: FileSequenceNo,
  size: Int = 1
)
object FileTransmissionFile {
  implicit val urlFormat = HttpUrlFormat
  implicit val writes = Json.writes[FileTransmissionFile]
}

case class FileTransmissionInterface(
  name: String,
  version: String
)
object FileTransmissionInterface {
  implicit var writes = Json.writes[FileTransmissionInterface]
}

case class FileTransmissionProperty(name: String, value: String)
object FileTransmissionProperty {
  implicit var writes = Json.writes[FileTransmissionProperty]
}

case class FileTransmission(
  batch: FileTransmissionBatch,
  callbackUrl: URL,
  file: FileTransmissionFile,
  interface: FileTransmissionInterface,
  properties: Seq[FileTransmissionProperty]
)
object FileTransmission {
  implicit val urlFormat = HttpUrlFormat
  implicit val writes = Json.writes[FileTransmission]
}
