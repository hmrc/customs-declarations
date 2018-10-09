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

import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.model.FileTransmissionNotification
import uk.gov.hmrc.customs.declaration.services.{BatchFileNotificationService, FileTransmissionCallbackToXmlNotification}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FileTransmissionNotificationController @Inject() (fileTransmissionCallBackToXmlNotification: FileTransmissionCallbackToXmlNotification,
                                                        notificationService: BatchFileNotificationService,
                                                        cdsLogger: CdsLogger) extends BaseController {

  def post(clientSubscriptionId: String): Action[AnyContent] = Action.async {
    request =>

      request.body.asJson.fold(
        {
          cdsLogger.error(s"Malformed JSON received. Body: ${request.body.asText} headers: ${request.headers}")
          Future.successful(errorBadRequest(errorMessage = "Invalid JSON payload").JsonResult)
        }
      ) { js =>
        js.validate[FileTransmissionNotification] match {
          case n: JsSuccess[FileTransmissionNotification] =>
            cdsLogger.debug(s"Valid JSON request received. Body=$js headers=${request.headers}")
            notificationService.sendMessage(n.value, clientSubscriptionId)(fileTransmissionCallBackToXmlNotification).map{ _ =>
              NoContent //convoId?
            }.recover {
              case e: Throwable =>
                cdsLogger.error(s"[conversationId=${n.value.reference.toString}][clientSubscriptionId=$clientSubscriptionId] file transmission notification service request to customs notification failed.", e)
                ErrorResponse.ErrorInternalServerError.JsonResult //convoId?
            }
          case _: JsError =>
            cdsLogger.error(s"Invalid JSON received. Body: ${request.body.asText} headers: ${request.headers}")
            Future.successful(errorBadRequest(errorCode = "BAD_REQUEST", errorMessage = "Invalid file upload outcome").JsonResult)
        }
      }
  }
}