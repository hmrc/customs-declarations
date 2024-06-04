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

package uk.gov.hmrc.customs.declaration.controllers

import play.api.Logger
import play.api.http.ContentTypes
import play.api.mvc._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, HasConversationId}
import uk.gov.hmrc.customs.declaration.model.{ConversationId, Mrn}
import uk.gov.hmrc.customs.declaration.services.DeclarationStatusService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeclarationStatusController @Inject()(val shutterCheckAction: ShutterCheckAction,
                                            val validateAndExtractHeadersStatusAction: ValidateAndExtractHeadersStatusAction,
                                            val authAction: AuthStatusAction,
                                            val conversationIdAction: ConversationIdAction,
                                            val declarationStatusService: DeclarationStatusService,
                                            cc: ControllerComponents,
                                            val logger: DeclarationsLogger)
                                           (implicit val ec: ExecutionContext)
  extends BackendController(cc) {

  def get(mrn: String): Action[AnyContent] = (
    Action andThen
      conversationIdAction andThen
      shutterCheckAction andThen
      validateAndExtractHeadersStatusAction andThen
      authAction
    ).async {

      implicit ar: AuthorisedRequest[AnyContent] =>

        logger.debug(s"Declaration status request received. Path = ${ar.path} \nheaders = ${ar.headers.headers}")

        declarationStatusService.send(Mrn(mrn)) map {
          case Right(res) =>
            val id = new HasConversationId {
              override val conversationId: ConversationId = ar.conversationId
            }
            logger.info(s"Declaration status request processed successfully.")(id)
            logger.debug(s"Returning filtered declaration status request with status code 200 and body\n${res.body}")(id)
            Ok(res.body).withConversationId(id).as(ContentTypes.XML)
          case Left(errorResult) =>
            errorResult
        }
    }

}
 class TestLogController@Inject()(val logger: DeclarationsLogger,
                                  cc: ControllerComponents)
                                 (implicit val ec: ExecutionContext)
   extends BackendController(cc) {
   def get(): Action[AnyContent] = Action {
     Logger(getClass()).info("""json{"conversationId":"abcde"}""")
     Ok
   }
 }
