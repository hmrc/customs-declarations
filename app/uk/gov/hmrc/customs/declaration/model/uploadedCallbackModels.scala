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
import java.time.Instant

import play.api.libs.json._

sealed trait UploadedFileStatus {
  val status: String
}
case object ReadyFileStatus extends UploadedFileStatus {
  override val status: String = "READY"
}
case object FailedFileStatus extends UploadedFileStatus {
  override val status: String = "FAILED"
}

object UploadedFileStatus {
  implicit val reads: Reads[UploadedFileStatus] = new Reads[UploadedFileStatus] {
    override def reads(json: JsValue): JsResult[UploadedFileStatus] = json match {
      case JsString(ReadyFileStatus.status) => JsSuccess(ReadyFileStatus)
      case JsString(FailedFileStatus.status) => JsSuccess(FailedFileStatus)
      case _ => JsError(s"Invalid UploadedFileStatus $json")
    }
  }
}

case class UploadedDetails(fileName: String, fileMimeType: String, uploadTimestamp: Instant, checksum: String)
object UploadedDetails {
  implicit val readsUploadDetails: Reads[UploadedDetails] = Json.reads[UploadedDetails]
}

case class UploadedErrorDetails(failureReason: String, message: String)
object UploadedErrorDetails {
  implicit val readsErrorDetails: Reads[UploadedErrorDetails] = Json.reads[UploadedErrorDetails]
}

sealed trait UploadedCallbackBody {
  val reference: FileReference
  val fileStatus: UploadedFileStatus
}

case class UploadedCallbackDecider(fileStatus: UploadedFileStatus)
object UploadedCallbackDecider{
  implicit val reads = Json.reads[UploadedCallbackDecider]
}

case class UploadedReadyCallbackBody(
                              reference: FileReference,
                              downloadUrl: URL,
                              fileStatus: UploadedFileStatus = ReadyFileStatus,
                              uploadDetails: UploadedDetails
                            ) extends UploadedCallbackBody

object UploadedReadyCallbackBody {
  implicit val urlFormat = HttpUrlFormat
  implicit val readsReadyCallback: Reads[UploadedReadyCallbackBody] = Json.reads[UploadedReadyCallbackBody]

  def parse(json: JsValue)(implicit reads: Reads[UploadedCallbackDecider]): JsResult[UploadedCallbackBody] = {
    json.validate[UploadedCallbackDecider] match {
      case JsSuccess(decider, _) => decider.fileStatus match {
        case ReadyFileStatus => json.validate[UploadedReadyCallbackBody]
        case FailedFileStatus => json.validate[UploadedFailedCallbackBody]
      }
      case error: JsError => error
    }
  }
}

case class UploadedFailedCallbackBody(
                               reference: FileReference,
                               fileStatus: UploadedFileStatus = FailedFileStatus,
                               failureDetails: UploadedErrorDetails
                             ) extends UploadedCallbackBody

object UploadedFailedCallbackBody {
  implicit val readsFailedCallback: Reads[UploadedFailedCallbackBody] = Json.reads[UploadedFailedCallbackBody]
}
