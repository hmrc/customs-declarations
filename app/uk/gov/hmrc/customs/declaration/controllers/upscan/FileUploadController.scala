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

package uk.gov.hmrc.customs.declaration.controllers.upscan

import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.mvc._
import uk.gov.hmrc.customs.declaration.controllers.Common
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.upscan.FileUploadPayloadValidationComposedAction
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{AuthActionEoriHeader, ConversationIdAction}
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedFileUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.services.upscan.FileUploadBusinessService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class FileUploadController @Inject()(val common: Common,
                                     val fileUploadBusinessService: FileUploadBusinessService,
                                     val fileUploadPayloadValidationComposedAction: FileUploadPayloadValidationComposedAction,
                                     val conversationIdAction: ConversationIdAction,
                                     val fileUploadAuthAction: AuthActionEoriHeader)
                                    (implicit ec: ExecutionContext)
  extends BackendController(common.cc) {

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) =>
      Right(AnyContentAsXml(xml))
    case _ =>
      Right(AnyContentAsEmpty)
  })

  def post(): Action[AnyContent] = (
    Action andThen
      conversationIdAction andThen
      common.shutterCheckAction andThen
      common.validateAndExtractHeadersAction andThen
      fileUploadAuthAction andThen
      fileUploadPayloadValidationComposedAction
    ).async(bodyParser = xmlOrEmptyBody) {

    implicit validatedRequest: ValidatedFileUploadPayloadRequest[AnyContent] =>
      val logger = common.logger

      logger.debug(s"File upload initiate request received. Payload=${validatedRequest.xmlBody} headers=${validatedRequest.headers.headers}")

      fileUploadBusinessService.send map {
        case Right(res) =>
            logger.info("Upload initiate request processed successfully")
          Ok(res).withConversationId.as(ContentTypes.XML)
        case Left(errorResult) =>
          errorResult
      }
  }
}
