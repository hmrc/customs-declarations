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

package uk.gov.hmrc.customs.declaration.controllers.filetransmission

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.declaration.controllers.{CommonSubmitterHeader, ErrorResponse}
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.filetransmission.{FileTransmissionCallbackDecider, FileTransmissionNotification}
import uk.gov.hmrc.customs.declaration.services.filetransmission.FileTransmissionCallbackToXmlNotification
import uk.gov.hmrc.customs.declaration.services.upscan.FileUploadNotificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class FileTransmissionNotificationController @Inject()(callbackToXmlNotification: FileTransmissionCallbackToXmlNotification,
                                                       common : CommonSubmitterHeader,
                                                       notificationService: FileUploadNotificationService,
                                                       cc: ControllerComponents,
                                                       cdsLogger: CdsLogger)
                                                      (implicit ec: ExecutionContext) extends BackendController(common.cc) {

  def post(clientSubscriptionIdString: String): Action[AnyContent] = Action.async {

    implicit request =>

      Try(UUID.fromString(clientSubscriptionIdString)) match {
        case Success(csid) =>
          val clientSubscriptionId = SubscriptionFieldsId(csid)
          request.body.asJson.fold(
            {
              cdsLogger.error(s"Malformed JSON received. Body: ${request.body.asText} headers: ${request.headers}")
              Future.successful(errorBadRequest(errorMessage = "Invalid JSON payload").JsonResult)
            }
          ) { js =>
            FileTransmissionCallbackDecider.parse(js) match {
              case JsSuccess(callbackBody, _) => callbackBody match {
                case notification: FileTransmissionNotification =>
                  cdsLogger.debug(s"Valid JSON success request received. Body=${Json.prettyPrint(js)} headers=${request.headers}")
                  println("controller--------" + request.headers)
                  notificationService.sendMessage[FileTransmissionNotification](notification, notification.fileReference, clientSubscriptionId)(callbackToXmlNotification, hc).map { _ =>
                    NoContent
                  }.recover {
                    case e: Throwable =>
                      handleException(e, notification, clientSubscriptionId)
                  }
              }
              case _: JsError =>
                cdsLogger.error(s"Invalid JSON received. Body: ${request.body.asText} headers: ${request.headers}")
                Future.successful(errorBadRequest(errorMessage = "Invalid file upload outcome").JsonResult)
            }
          }
        case Failure(e) =>
          cdsLogger.error("Invalid clientSubscriptionId", e)
          Future.successful(errorBadRequest(errorMessage = "Invalid clientSubscriptionId").JsonResult)
      }
  }

  private def handleException(e: Throwable, notification: FileTransmissionNotification, clientSubscriptionId: SubscriptionFieldsId) = {
    cdsLogger.error(s"[conversationId=${notification.fileReference.toString}][clientSubscriptionId=${clientSubscriptionId.toString}] file transmission notification service request to customs notification failed.", e)
    ErrorResponse.ErrorInternalServerError.JsonResult
  }

}