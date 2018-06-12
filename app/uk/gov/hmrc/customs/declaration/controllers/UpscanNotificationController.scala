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

package uk.gov.hmrc.customs.declaration.controllers

import java.util.UUID

import com.google.inject.Inject
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Results}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declaration.controllers.FileStatus.FileStatus
import uk.gov.hmrc.customs.declaration.services.{UploadedFileDetails, UploadedFileProcessingService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future

object FileStatus extends Enumeration {
  type FileStatus = Value
  val READY, FAILED = Value

  implicit val fileStatusReads = Reads.enumNameReads(FileStatus)
}

case class UploadDetails(uploadTimestamp: Option[String], checksum: Option[String])
case class FailureDetails(failureReason: Option[String], message: Option[String])
case class UpscanNotification(reference: UUID, fileStatus: FileStatus, uploadDetails: Option[UploadDetails], failureDetails: Option[FailureDetails], url: Option[String])

object UpscanNotification {

  implicit val uploadDetailsReads = (

    (JsPath \\ "uploadTimestamp").readNullable[String]
      .filter(ValidationError("File status is READY so uploadTimestamp is required"))(_.isDefined)
      and
      (JsPath \\ "checksum").readNullable[String]
        .filter(ValidationError("File status is READY so checksum is required"))(_.isDefined)
    ) (UploadDetails.apply _)

  implicit val failureDetailsReads = (
    (JsPath \\ "failureReason").readNullable[String].filter(ValidationError("File status is FAILED so failureReason is required"))(_.isDefined)
      and
      (JsPath \\ "message").readNullable[String].filter(ValidationError("File status is FAILED so message is required"))(_.isDefined)
    ) (FailureDetails.apply _)

  implicit val upscanNotificationReads: Reads[UpscanNotification] = (
    (JsPath \ "reference").read[UUID] and
      (JsPath \ "fileStatus").read[FileStatus] and

      (JsPath \ "fileStatus").read[FileStatus].flatMap {
        case FileStatus.FAILED => {
          new Reads[Option[UploadDetails]] {
            def reads(js: JsValue) = JsSuccess(None)
          }
        }
        case FileStatus.READY => {
          new Reads[Option[UploadDetails]] {
            def reads(js: JsValue) = {
              js.validateOpt[UploadDetails](uploadDetailsReads)
            }
          }
        }
      } and
      (JsPath \ "fileStatus").read[FileStatus].flatMap {
        case FileStatus.FAILED => {
          new Reads[Option[FailureDetails]] {
            def reads(js: JsValue) = {
              js.validateOpt[FailureDetails](failureDetailsReads)
            }
          }
        }
        case FileStatus.READY => {
          new Reads[Option[FailureDetails]] {
            def reads(js: JsValue) = JsSuccess(None)
          }
        }
      } and
      (JsPath \ "url").readNullable[String]
    ) (UpscanNotification.apply _)
}

class UpscanNotificationController @Inject()(downstreamService: UploadedFileProcessingService) extends BaseController {
  def post(decId: String, eori: String, docType: String, clientSubscriptionId: String): Action[AnyContent] = Action.async { request =>
    request.body.asJson
      .fold(Future.successful(errorBadRequest(errorMessage = "Invalid JSON payload").JsonResult)) { js =>
        js.validate[UpscanNotification] match {
          case n: JsSuccess[UpscanNotification] => {
            downstreamService.sendMessage(UploadedFileDetails(decId, eori, docType, clientSubscriptionId, n.value))
            Future.successful(Results.NoContent)
          }
          case e: JsError => Future.successful(errorBadRequest(errorCode = "BAD_REQUEST", errorMessage = e.errors.toString()).JsonResult)
        }
      }
  }
}
