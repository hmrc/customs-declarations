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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{ActionTransformer, Request}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.declaration.services.{DateTimeService, UniqueIdsService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConversationIdAction @Inject() (val logger: DeclarationsLogger,
                                      val correlationIdService: UniqueIdsService,
                                      val timeService: DateTimeService)
                                     (implicit ec: ExecutionContext)
  extends ActionTransformer[Request, ConversationIdRequest] {

  override def executionContext: ExecutionContext = ec
  override def transform[A](request: Request[A]): Future[ConversationIdRequest[A]] = {

    val r = ConversationIdRequest(correlationIdService.conversation, timeService.zonedDateTimeUtc, request)
    logger.debugFull("In ConversationIdAction")(r)
    logger.infoRaw("TEST MESSAGE")
    logger.infoRaw("""json{"cds.conversationId":"xyzxyz"}""")

    Future.successful(r)
  }
}
