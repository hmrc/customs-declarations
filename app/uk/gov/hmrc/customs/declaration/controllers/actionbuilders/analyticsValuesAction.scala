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
import play.api.mvc.{ActionTransformer, Request}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsValues
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsValues._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AnalyticsValuesAndConversationIdRequest
import uk.gov.hmrc.customs.declaration.services.{DateTimeService, UniqueIdsService}

import scala.concurrent.Future

abstract class EndpointAction() extends ActionTransformer[Request, AnalyticsValuesAndConversationIdRequest] {

  val logger: DeclarationsLogger
  val googleAnalyticsValues: GoogleAnalyticsValues
  val correlationIdService: UniqueIdsService
  val timeService: DateTimeService

  override def transform[A](request: Request[A]): Future[AnalyticsValuesAndConversationIdRequest[A]] = {

    val r = AnalyticsValuesAndConversationIdRequest(correlationIdService.conversation, googleAnalyticsValues, timeService.zonedDateTimeUtc, request)
    logger.debugFull("In AnalyticsValuesAction.")(r)

    Future.successful(r)
  }
}

@Singleton
class FileUploadAnalyticsValuesAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction {
  override val googleAnalyticsValues: GoogleAnalyticsValues = Fileupload
}

@Singleton
class BatchFileUploadAnalyticsValuesAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction {
  override val googleAnalyticsValues: GoogleAnalyticsValues = BatchFileUpload //TODO can existing `Fileupload` values be used?
}

@Singleton
class DeclarationSubmitAnalyticsValuesAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction {
  override val googleAnalyticsValues: GoogleAnalyticsValues = Submit
}

@Singleton
class DeclarationClearanceAnalyticsValuesAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction {
  override val googleAnalyticsValues: GoogleAnalyticsValues = Clearance
}

@Singleton
class DeclarationCancellationAnalyticsValuesAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction {
  override val googleAnalyticsValues: GoogleAnalyticsValues = Cancel
}

@Singleton
class DeclarationAmendValuesAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction {
  override val googleAnalyticsValues: GoogleAnalyticsValues = Amend
}

@Singleton
class DeclarationArrivalNotificationValuesAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction {
  override val googleAnalyticsValues: GoogleAnalyticsValues = ArrivalNotification
}

@Singleton
class DeclarationStatusValuesAction @Inject()(override val logger: DeclarationsLogger, override val correlationIdService: UniqueIdsService, override val timeService: DateTimeService) extends EndpointAction {
  override val googleAnalyticsValues: GoogleAnalyticsValues = DeclarationStatus
}
