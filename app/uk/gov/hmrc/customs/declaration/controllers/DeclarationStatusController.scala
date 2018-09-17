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
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedStatusRequest, HasConversationId}
import uk.gov.hmrc.customs.declaration.model.{ConversationId, Mrn}
import uk.gov.hmrc.customs.declaration.services.DeclarationStatusService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DeclarationStatusController @Inject()(val validateAndExtractHeadersStatusAction: ValidateAndExtractHeadersStatusAction,
                                            val authAction: AuthStatusAction,
                                            val declarationStatusValuesAction: DeclarationStatusValuesAction,
                                            val declarationStatusService: DeclarationStatusService,
                                            val logger: DeclarationsLogger,
                                            val googleAnalyticsConnector: GoogleAnalyticsConnector) extends BaseController {

  def get(mrn: String): Action[AnyContent] = (
    Action andThen
      declarationStatusValuesAction andThen
      validateAndExtractHeadersStatusAction andThen
      authAction
    ).async {

      implicit asr: AuthorisedStatusRequest[AnyContent] =>

        logger.debug(s"Declaration status request  received. Payload = ${asr.body.toString} headers = ${asr.headers.headers}")

        declarationStatusService.send(Mrn(mrn)) map {
          case Right(res) =>
            val id = new HasConversationId {
              override val conversationId: ConversationId = asr.conversationId
            }
            logger.info(s"Declaration status request processed successfully.")(id)
            googleAnalyticsConnector.success
            Ok(res.body).withConversationId(id)
          case Left(errorResult) =>
            errorResult
        }
    }
}
