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

package uk.gov.hmrc.customs.declaration.controllers.actionbuilders

import play.api.mvc.*
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ApiVersionRequest, ValidatedHeadersStatusRequest}

import javax.inject.{Inject, Singleton}
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

/** Action builder that validates headers.
  * <li/>INPUT - `ApiVersionRequest`
  * <li/>OUTPUT - `ValidatedHeadersStatusRequest`
  * <li/>ERROR - 4XX Result if is a header validation error. This terminates the action builder pipeline.
  */
@Singleton
class ValidateAndExtractHeadersStatusAction @Inject()(validator: HeaderStatusValidator,
                                                      @unused logger: DeclarationsLogger)
                                                     (implicit ec: ExecutionContext)
  extends ActionRefiner[ApiVersionRequest, ValidatedHeadersStatusRequest] {
  actionName =>

  override def executionContext: ExecutionContext = ec
  override def refine[A](avr: ApiVersionRequest[A]): Future[Either[Result, ValidatedHeadersStatusRequest[A]]] = Future.successful {
    implicit val id: ApiVersionRequest[A] = avr

    validator.validateHeaders(avr) match {
      case Left(result) =>
        Left(result.XmlResult.withConversationId)
      case Right(extracted) =>
        val vhr = ValidatedHeadersStatusRequest(avr.conversationId, avr.start, avr.requestedApiVersion,extracted.badgeIdentifier, extracted.clientId, avr.request)
        Right(vhr)
    }
  }
}
