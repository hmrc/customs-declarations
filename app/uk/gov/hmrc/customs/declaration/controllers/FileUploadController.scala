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

import javax.inject.{Inject, Singleton}

import play.api.mvc._
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{FileUploadAnalyticsValuesAction, FileUploadAuthAction, FileUploadPayloadValidationComposedAction}
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedFileUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.services.FileUploadBusinessService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class FileUploadController @Inject()(val common: Common,
                                     val fileUploadBusinessService: FileUploadBusinessService,
                                     val fileUploadPayloadValidationComposedAction: FileUploadPayloadValidationComposedAction,
                                     val fileUploadAnalyticsValuesAction: FileUploadAnalyticsValuesAction,
                                     val fileUploadAuthAction: FileUploadAuthAction,
                                     val googleAnalyticsConnector: GoogleAnalyticsConnector)
  extends BaseController {

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) =>
      Right(AnyContentAsXml(xml))
    case _ =>
      Right(AnyContentAsEmpty)
  })

  def post(): Action[AnyContent] = (
    Action andThen
      fileUploadAnalyticsValuesAction andThen
      common.validateAndExtractHeadersAction andThen
      fileUploadAuthAction andThen
      fileUploadPayloadValidationComposedAction
    ).async(bodyParser = xmlOrEmptyBody) {

    implicit validatedRequest: ValidatedFileUploadPayloadRequest[AnyContent] =>
      val logger = common.logger

      logger.debug(s"Batch file upload initiate request received. Payload=${validatedRequest.body.toString} headers=${validatedRequest.headers.headers}")

      fileUploadBusinessService.send map {
        case Right(res) =>
          logger.info("Upload initiate request processed successfully")
          googleAnalyticsConnector.success
          Ok(res).withConversationId
        case Left(errorResult) =>
          errorResult
      }
  }
}
