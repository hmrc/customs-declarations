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

package uk.gov.hmrc.customs.declaration.controllers.actionbuilders

import javax.inject.{Inject, Singleton}
import play.api.mvc.{ActionTransformer, Request}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.declaration.services.{DateTimeService, UniqueIdsService}

import scala.concurrent.Future

abstract class EndpointAction() extends ActionTransformer[Request, ConversationIdRequest] {

  val logger: DeclarationsLogger
  val correlationIdService: UniqueIdsService
  val timeService: DateTimeService

  override def transform[A](request: Request[A]): Future[ConversationIdRequest[A]] = {

    val r = ConversationIdRequest(correlationIdService.conversation, timeService.zonedDateTimeUtc, request)
    logger.debugFull("In ConversationIdAction.")(r)

    Future.successful(r)
  }
}

@Singleton
class FileUploadConversationIdAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction

@Singleton
class DeclarationSubmitConversationIdAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction

@Singleton
class DeclarationClearanceConversationIdAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction

@Singleton
class DeclarationCancellationConversationIdAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction

@Singleton
class DeclarationAmendConversationIdAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction

@Singleton
class DeclarationArrivalNotificationConversationIdAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction

@Singleton
class DeclarationStatusConversationIdAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction
