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

import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AuthorisedStatusRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left
import scala.util.control.NonFatal

@Singleton
class DeclarationStatusService @Inject()(logger: DeclarationsLogger,
                                         apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                         declarationStatusConnector: DeclarationStatusConnector,
                                         config: DeclarationsConfigService,
                                         dateTimeProvider: DateTimeService,
                                         uniqueIdsService: UniqueIdsService) {

  def send[A](mrn: Mrn)(implicit asr: AuthorisedStatusRequest[A], hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {

    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation
    val dmirId = uniqueIdsService.dmir

    declarationStatusConnector.send(dateTime, correlationId, dmirId, asr.requestedApiVersion, mrn)
      .map(f => Right(f)).recover{
      case NonFatal(e) =>
        logger.error(s"Upscan initiate call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }
}
