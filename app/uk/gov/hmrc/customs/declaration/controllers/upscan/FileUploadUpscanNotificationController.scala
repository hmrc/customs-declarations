/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.controllers.upscan

import java.util.UUID

import com.google.inject.Inject
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.upscan.{FileReference, UploadedCallbackBody, UploadedFailedCallbackBody, UploadedReadyCallbackBody}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.customs.declaration.services.upscan.{FileUploadNotificationService, FileUploadUpscanNotificationBusinessService, UpscanNotificationCallbackToXmlNotification}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class FileUploadUpscanNotificationController @Inject()(notificationService: FileUploadNotificationService,
                                                       toXmlNotification: UpscanNotificationCallbackToXmlNotification,
                                                       errorToXmlNotification: InternalErrorXmlNotification,
                                                       businessService: FileUploadUpscanNotificationBusinessService,
                                                       cc: ControllerComponents,
                                                       cdsLogger: CdsLogger)
                                                      (implicit ec: ExecutionContext) extends BackendController(cc) {

  def post(clientSubscriptionIdString: String): Action[AnyContent] = Action.async { implicit request =>

    Try(UUID.fromString(clientSubscriptionIdString)) match {
      case Success(csid) =>
        val clientSubscriptionId = SubscriptionFieldsId(csid)
        request.body.asJson
          .fold{
              cdsLogger.error(s"Malformed JSON received. Body: ${request.body.asText} headers: ${request.headers}")
              Future.successful(errorBadRequest(errorMessage = "Invalid JSON payload").JsonResult)
          }{js =>
            UploadedReadyCallbackBody.parse(js) match {
              case JsSuccess(callbackBody, _) =>
                implicit val conversationId = conversationIdForLogging(callbackBody.reference.value)
                callbackBody match {
                  case ready: UploadedReadyCallbackBody =>
                    cdsLogger.debug(s"Valid JSON request received with READY status. Body: ${Json.prettyPrint(js)} headers: ${request.headers}")
                    businessService.persistAndCallFileTransmission(clientSubscriptionId, ready).map{_ =>
                        Results.NoContent
                    }.recover{
                      case e: Throwable =>
                        asyncNotifyInternalServerError(ready, clientSubscriptionId)
                        internalServerErrorResult(e)
                    }
                  case failed: UploadedFailedCallbackBody =>
                    cdsLogger.debug(s"Valid JSON request received with FAILED status. Body: ${Json.prettyPrint(js)} headers: ${request.headers}")
                    notificationService.sendMessage[UploadedFailedCallbackBody](
                      failed,
                      failed.reference,
                      clientSubscriptionId
                    )(toXmlNotification)
                    .map( _ => Results.NoContent ).recover{
                      case e: Throwable =>
                        internalServerErrorResult(e)
                    }
              }
              case e: JsError =>
                cdsLogger.error(s"Invalid JSON received. Body: ${request.body.asText} headers: ${request.headers}\nerror=${e.errors.toString()}")
                Future.successful(errorBadRequest(errorMessage = "Invalid upscan notification").JsonResult)
            }
          }
      case Failure(e) =>
        cdsLogger.error("Invalid clientSubscriptionId", e)
        Future.successful(errorBadRequest(errorMessage = "Invalid clientSubscriptionId").JsonResult)
    }
  }

  private def conversationIdForLogging(uuid: UUID) = {
    new HasConversationId {
      override val conversationId: ConversationId = ConversationId(uuid)
    }
  }

  private def internalServerErrorResult(e: Throwable)(implicit request: Request[AnyContent]) = {
    cdsLogger.error(s"Error processing file transmission callback request. Body: ${request.body.asText} headers: ${request.headers}", e)
    ErrorInternalServerError.JsonResult
  }

  private def asyncNotifyInternalServerError(callbackBody: UploadedCallbackBody, subscriptionFieldsId: SubscriptionFieldsId)(implicit request: Request[AnyContent]) = {
    //TODO: Use persistent retry when this is available
    Future {
      notificationService.sendMessage[FileReference](
        callbackBody.reference,
        callbackBody.reference,
        subscriptionFieldsId
      )(errorToXmlNotification).recover{
        case e: Throwable =>
          cdsLogger.error(s"Error sending internal error notification. Body: ${request.body.asText} headers: ${request.headers}", e)
      }
    }
  }

}
