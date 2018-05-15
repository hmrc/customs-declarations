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

import javax.inject.{Inject, Singleton}
import play.api.http.MimeTypes
import play.api.libs.json.{Json, OFormat}
import play.api.mvc._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.model.{ConversationId, InitiateUpscanResponsePayload, InitiateUpscanUploadRequest}
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.services.FileUploadBusinessService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class FileUploadController @Inject()(
                                      common: Common,
                                      fileUploadBusinessService: FileUploadBusinessService,
                                      fileUploadPayloadValidationComposedAction: FileUploadPayloadValidationComposedAction
)
extends BaseController {

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) =>
      Right(AnyContentAsXml(xml))
    case _ =>
      Right(AnyContentAsEmpty)
  })

  def post(): Action[AnyContent] = (
    Action andThen
      common.conversationIdAction andThen
      common.validateAndExtractHeadersAction andThen
      common.authAction andThen
      fileUploadPayloadValidationComposedAction
    )
    .async(bodyParser = xmlOrEmptyBody) {

      implicit vupr: ValidatedUploadPayloadRequest[AnyContent] =>
      val logger = common.logger

      logger.debug(s"Request received. Payload = ${vupr.body.toString} headers = ${vupr.headers.headers}")

        fileUploadBusinessService.send map {
          case Right(res) =>
            logger.info(s"Upload initiate request processed successfully")
            implicit val conversationId = ConversationId(UUID.fromString(res.reference))
            val request: InitiateUpscanUploadRequest = res.uploadRequest
            Ok(request.toXml).as(MimeTypes.XML).withConversationId
        case Left(errorResult) =>
          errorResult
      }
    }
}
