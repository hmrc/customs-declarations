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

import play.api.http.MimeTypes
import play.api.mvc._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.BusinessService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class Common @Inject() (
  val conversationIdAction: ConversationIdAction,
  val authAction: CspAndThenNonCspAuthAction,
  val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction,
  val businessService: BusinessService,
  val logger: DeclarationsLogger
)

@Singleton
class SubmitDeclarationController @Inject()(
  common: Common,
  payloadValidationAction: SubmitPayloadValidationAction
) extends CustomsDeclarationController(common, payloadValidationAction)

@Singleton
class CancelDeclarationController @Inject()(
  common: Common,
  payloadValidationAction: CancelPayloadValidationAction
) extends CustomsDeclarationController(common, payloadValidationAction)

abstract class CustomsDeclarationController(
  common: Common,
  payloadValidationAction: PayloadValidationAction
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
      common.authAction.authAction andThen
      payloadValidationAction
    )
    .async(bodyParser = xmlOrEmptyBody) {

      implicit vpr: ValidatedPayloadRequest[AnyContent] =>
        val logger = common.logger

        logger.debug(s"Request received. Payload = ${vpr.body.toString} headers = ${vpr.headers.headers}")
        logger.info(s"Declaration request received")

        common.businessService.send map {
          case Right(_) =>
            Accepted.as(MimeTypes.XML).withConversationId
          case Left(errorResult) =>
            errorResult
        }

    }
}
