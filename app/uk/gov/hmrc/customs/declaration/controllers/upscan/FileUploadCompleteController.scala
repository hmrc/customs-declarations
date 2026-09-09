/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc.*
import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declaration.controllers.Common
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{AuthActionEoriHeader, ConversationIdAction}
import uk.gov.hmrc.customs.declaration.model.upscan.{BatchId, FileReference}
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.services.upscan.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.net.URLEncoder
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class FileUploadCompleteController @Inject()(val common: Common,
                                             batchCompletionService: BatchCompletionService,
                                             apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                             conversationIdAction: ConversationIdAction,
                                             fileUploadAuthAction: AuthActionEoriHeader)
                                            (using ExecutionContext)
  extends BackendController(common.cc) {

  private val apiContextEncoded = URLEncoder.encode("customs/declarations", "UTF-8")

  def post() = (
    Action andThen
      conversationIdAction andThen
      common.shutterCheckAction andThen
      common.validateAndExtractHeadersAction andThen
      fileUploadAuthAction
    ).async(parse.xml) { implicit request =>

    val xml = request.body
    val batchId = BatchId(UUID.fromString((xml \ "BatchID").text.trim)) //TODO should batch id be in the path?
    val references = (xml \ "Files" \ "File" \ "Reference").map(node => FileReference(UUID.fromString(node.text.trim)))
    val authorisedEori = extractEori(request.authorisedAs)

    apiSubFieldsConnector
      .getSubscriptionFields(ApiSubscriptionKey(request.clientId, apiContextEncoded, request.requestedApiVersion))
      .flatMap { response =>
        //TODO clean up mongo after completing
        batchCompletionService.complete(batchId, SubscriptionFieldsId(response.fieldsId), authorisedEori, references)
          .map(_ => Ok("Completed"))
      }
  }

  private def extractEori(authorisedAs: AuthorisedAs): Option[Eori] = authorisedAs match {
    case nonCsp: NonCsp => Some(nonCsp.eori)
    case csp: Csp => csp.eori
  }
}
