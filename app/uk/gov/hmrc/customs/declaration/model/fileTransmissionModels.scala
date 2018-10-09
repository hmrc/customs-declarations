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

import play.api.libs.json._

import scala.xml.NodeSeq

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

sealed trait FileTransmissionOutcome {
  val outcome: String
}
case object FileTransmissionSuccessOutcome extends FileTransmissionOutcome {
  override val outcome: String = "SUCCESS"
}
case object FileTransmissionFailureOutcome extends FileTransmissionOutcome {
  override val outcome: String = "FAILURE"
}

object FileTransmissionOutcome {
  implicit val readsFileTransmissionOutcome: Reads[FileTransmissionOutcome] = new Reads[FileTransmissionOutcome] {
    override def reads(json: JsValue): JsResult[FileTransmissionOutcome] = json match {
      case JsString(FileTransmissionSuccessOutcome.outcome) => JsSuccess(FileTransmissionSuccessOutcome)
      case JsString(FileTransmissionFailureOutcome.outcome) => JsSuccess(FileTransmissionFailureOutcome)
      case _ => JsError(s"Invalid FileTransmissionOutcome $json")
    }
  }
}

//case class FileTransmissionNotification(fileReference: FileReference,
//                                        batchId: BatchId,
//                                        outcome: FileTransmissionOutcome,
//                                        errorDetails: Option[String]) extends CallbackResponse {
//  override def reference: FileReference = fileReference
//}
//
//object FileTransmissionNotification {
//  implicit val readsFileTransmissionNotification: Reads[FileTransmissionNotification] = Json.reads[FileTransmissionNotification]
//}

sealed trait FileTransmissionNotification extends CallbackResponse {
  val reference: FileReference
  val batchId: BatchId
  val outcome: FileTransmissionOutcome
  val errorDetails: Option[String]
}

trait CallbackToXmlNotification[A <: CallbackResponse] {
  def toXml(callbackResponse: A): NodeSeq
}

trait CallbackResponse {
  def reference: FileReference
}

case class FileTransmissionCallbackDecider(outcome: FileTransmissionOutcome)

object FileTransmissionCallbackDecider {
  implicit val reads = Json.reads[FileTransmissionCallbackDecider]
  def parse(json: JsValue): JsResult[CallbackResponse] = {
    json.validate[FileTransmissionCallbackDecider] match {
      case JsSuccess(decider, _) => decider.outcome match {
        case FileTransmissionSuccessOutcome => json.validate[FileTransmissionSuccessNotification]
        case FileTransmissionFailureOutcome => json.validate[FileTransmissionSuccessNotification]
      }
      case error: JsError => error
    }
  }
}

case class FileTransmissionSuccessNotification(reference: FileReference,
                                               batchId: BatchId,
                                               outcome: FileTransmissionOutcome = FileTransmissionSuccessOutcome,
                                               errorDetails: Option[String]
                                    ) extends FileTransmissionNotification

object FileTransmissionSuccessNotification {
  implicit val readsSuccessCallback: Reads[FileTransmissionSuccessNotification] = Json.reads[FileTransmissionSuccessNotification]
}

case class FileTransmissionFailureNotification(override val reference: FileReference,
                                               batchId: BatchId,
                                               outcome: FileTransmissionOutcome = FileTransmissionFailureOutcome,
                                               errorDetails: Option[String]
                                     ) extends FileTransmissionNotification

object FileTransmissionFailureNotification {
  implicit val readsFailureCallback: Reads[FileTransmissionFailureNotification] = Json.reads[FileTransmissionFailureNotification]
}
