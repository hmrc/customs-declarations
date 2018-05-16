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
import uk.gov.hmrc.customs.declaration.services.{UploadedFileProcessingService, UploadedFileDetails}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future

object FileStatus extends Enumeration {
  type FileStatus = Value
  val READY, FAILED = Value

  implicit val fileStatusReads = Reads.enumNameReads(FileStatus)
}

case class UpscanNotification(reference: UUID, fileStatus: FileStatus, details: Option[String], url: Option[String])

object UpscanNotification {

  private def detailsMustBeProvidedWhenStatusIsFailed(fs: FileStatus, details: Option[String]) =
    fs == FileStatus.READY || (fs == FileStatus.FAILED && details.isDefined)


  private val detailsReads: Reads[Option[String]] = (JsPath \ "fileStatus").read[FileStatus].flatMap { fs =>
    (__ \ "details").readNullable[String]
      .filter(ValidationError("File status is FAILED so details are required"))(detailsMustBeProvidedWhenStatusIsFailed(fs, _))
  }

  implicit val UpscanNotificationReads: Reads[UpscanNotification] = (
    (JsPath \ "reference").read[UUID] and
      (JsPath \ "fileStatus").read[FileStatus] and
      detailsReads and
      (JsPath \ "url").readNullable[String]
    ).apply {
    UpscanNotification.apply _
  }
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
          case e: JsError => Future.successful(errorBadRequest(errorCode = "Unexpected JSON", errorMessage = e.errors.toString()).JsonResult)
        }
      }
  }

}
