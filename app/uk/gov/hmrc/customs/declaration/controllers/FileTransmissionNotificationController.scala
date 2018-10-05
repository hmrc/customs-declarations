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

import javax.inject.Inject

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Results}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declaration.controllers.FileTransmissionStatus.FileTransmissionStatus
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{BatchId, FileReference}
import uk.gov.hmrc.customs.declaration.services.FileTransmissionNotificationService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FileTransmissionStatus extends Enumeration {
  type FileTransmissionStatus = Value
  val SUCCESS, FAILURE = Value

  implicit val fileTransmissionStatusReads = Reads.enumNameReads(FileTransmissionStatus)
}

case class FileTransmissionNotification(fileReference: FileReference,
                                        batchId: BatchId,
                                        fileTransmissionStatus: FileTransmissionStatus,
                                        errorDetails: Option[String])
object FileTransmissionNotification {

  implicit val errorDetailsReads: Reads[Option[String]] = (JsPath \\ "errorDetails").readNullable[String].filter(ValidationError("Outcome is FAILURE so errorDetails are required"))(_.isDefined)

  implicit val fileTransmissionNotificationReads: Reads[FileTransmissionNotification] = (
    (JsPath \ "fileReference").read[FileReference] and
    (JsPath \ "batchId").read[BatchId] and
    (JsPath \ "outcome").read[FileTransmissionStatus] and

      (JsPath \ "outcome").read[FileTransmissionStatus].flatMap {
        case FileTransmissionStatus.SUCCESS => {
          new Reads[Option[String]] {
            def reads(js: JsValue) = JsSuccess(None)
          }
        }
//        case FailureFileTransmissionStatus => {
//          new Reads[Option[String]] {
//            def reads(js: JsValue) = {
//              js.validateOpt(errorDetailsReads)
//            }
//          }
//        }
      }
    ) (FileTransmissionNotification.apply _)
}

class FileTransmissionNotificationController @Inject() (notificationService: FileTransmissionNotificationService,
                                                        declarationsLogger: DeclarationsLogger) extends BaseController {

  def post(clientSubscriptionId: String): Action[AnyContent] = Action.async {
    request =>

      request.body.asJson.fold(
        {
          declarationsLogger.errorWithoutRequestContext(s"Malformed JSON received. Body: ${request.body.asText} headers: ${request.headers}")
          Future.successful(errorBadRequest(errorMessage = "Invalid JSON payload").JsonResult)
        }
      ) { js =>
        js.validate[FileTransmissionNotification] match {
          case n: JsSuccess[FileTransmissionNotification] => {
            declarationsLogger.debugWithoutRequestContext(s"Valid JSON request received. Body=$js headers=${request.headers}")
            notificationService.sendMessage(n.value, clientSubscriptionId).recover {
              case e: Throwable =>
                declarationsLogger.errorWithoutRequestContext(s"[conversationId=${n.value.fileReference.toString}][clientSubscriptionId=$clientSubscriptionId] File Transmission notification request to Customs Notification failed due to ${e.getMessage}")
                Future.successful(ErrorResponse.ErrorInternalServerError.JsonResult.withHeaders((CustomHeaderNames.XConversationIdHeaderName, n.value.fileReference.toString)))
            }
            Future.successful(Results.NoContent)
          }
          case _: JsError => {
            declarationsLogger.errorWithoutRequestContext(s"Invalid JSON received. Body: ${request.body.asText} headers: ${request.headers}")
            Future.successful(errorBadRequest(errorCode = "BAD_REQUEST", errorMessage = "Invalid file upload outcome").JsonResult)
          }
        }
      }
  }
}