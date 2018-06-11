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

package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId

import scala.concurrent.Future

@Singleton
class GoogleAnalyticsService @Inject()(logger: DeclarationsLogger,
                                       googleAnalyticsConnector: GoogleAnalyticsConnector) {

  private def send(event: String, message: String)(implicit hasConversationId: HasConversationId): Future[Unit] = {
    googleAnalyticsConnector.send(event, message)
  }

  def success(origin: String)(implicit hasConversationId: HasConversationId): Future[Unit] = {
    send(s"${origin}Success",s"ConversationId: ${hasConversationId.conversationId.toString}")
  }


  def failure[A](origin: String, errorMessage: String)(implicit hasConversationId: HasConversationId): Future[Unit] = {

    send(s"${origin}Failure",s"ConversationId: ${hasConversationId.conversationId.toString} $errorMessage")
  }
}
