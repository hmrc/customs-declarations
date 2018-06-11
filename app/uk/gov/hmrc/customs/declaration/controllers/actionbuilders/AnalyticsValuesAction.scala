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

package uk.gov.hmrc.customs.declaration.controllers.actionbuilders

import javax.inject.{Inject, Singleton}
import play.api.mvc.ActionTransformer
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsValues
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AnalyticsValuesAndConversationIdRequest, ConversationIdRequest}

import scala.concurrent.Future

abstract class AnalyticsValuesAction (logger: DeclarationsLogger, analyticsValues: GoogleAnalyticsValues) extends ActionTransformer[ConversationIdRequest, AnalyticsValuesAndConversationIdRequest] {

  override def transform[A](request: ConversationIdRequest[A]): Future[AnalyticsValuesAndConversationIdRequest[A]] = {

    val r = AnalyticsValuesAndConversationIdRequest(request.conversationId, analyticsValues , request.request)
    logger.debugFull("In AnalyticsValuesAction.")(r)

    Future.successful(r)
  }
}

@Singleton
class FileUploadAnalyticsValuesAction @Inject()(logger: DeclarationsLogger) extends AnalyticsValuesAction(logger, GoogleAnalyticsValues.Fileupload)

@Singleton
class DeclarationSubmitAnalyticsValuesAction @Inject()(logger: DeclarationsLogger) extends AnalyticsValuesAction(logger, GoogleAnalyticsValues.Submit)

@Singleton
class DeclarationClearanceAnalyticsValuesAction @Inject()(logger: DeclarationsLogger) extends AnalyticsValuesAction(logger, GoogleAnalyticsValues.Clearance)

@Singleton
class DeclarationAmendAnalyticsValuesAction @Inject()(logger: DeclarationsLogger) extends AnalyticsValuesAction(logger, GoogleAnalyticsValues.Amend)

@Singleton
class DeclarationCancellationAnalyticsValuesAction @Inject()(logger: DeclarationsLogger) extends AnalyticsValuesAction(logger, GoogleAnalyticsValues.Cancel)

@Singleton
class DeclarationAmendValuesAction @Inject()(logger: DeclarationsLogger) extends AnalyticsValuesAction(logger, GoogleAnalyticsValues.Amend)