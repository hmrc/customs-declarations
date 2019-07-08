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

package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}
import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorGenericBadRequest
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AuthorisedStatusRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left
import scala.util.control.NonFatal
import scala.xml.{Elem, PrettyPrinter, TopScope, XML}

@Singleton
class DeclarationStatusService @Inject()(statusResponseFilterService: StatusResponseFilterService,
                                         statusResponseValidationService: StatusResponseValidationService,
                                         override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                         override val logger: DeclarationsLogger,
                                         connector: DeclarationStatusConnector,
                                         dateTimeProvider: DateTimeService,
                                         uniqueIdsService: UniqueIdsService)
                                        (implicit val ec: ExecutionContext) extends ApiSubscriptionFieldsService {

  def send[A](mrn: Mrn)(implicit asr: AuthorisedStatusRequest[A], hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {

    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation
    val dmirId = uniqueIdsService.dmir

    futureApiSubFieldsId(asr.clientId) flatMap {
      case Right(sfId) =>
        connector.send(dateTime, correlationId, dmirId, asr.requestedApiVersion, sfId, mrn)
          .map(response => {
            val xmlResponseBody = XML.loadString(response.body)
            statusResponseValidationService.validate(xmlResponseBody, asr.badgeIdentifier) match {
              case Right(_) => Right(filterResponse(response, xmlResponseBody))
              case Left(errorResponse) =>
                logError(errorResponse)
                Left(errorResponse.XmlResult.withConversationId)
              case _ =>
                logError(ErrorGenericBadRequest)
                Left(ErrorGenericBadRequest.XmlResult.withConversationId)
            }
          }).recover{
          case e: RuntimeException if e.getCause.isInstanceOf[NotFoundException] =>
            logger.error(s"declaration status call failed with 404: ${e.getMessage}", e)
            Left(ErrorResponse.ErrorNotFound.XmlResult.withConversationId)
          case NonFatal(e) =>
            logger.error(s"declaration status call failed: ${e.getMessage}", e)
            Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
        }
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  private def logError[A](errorResponse: ErrorResponse)(implicit asr: AuthorisedStatusRequest[A]): Unit = {
    logger.error(s"declaration status call returning error response '${errorResponse.message}' and status code ${errorResponse.httpStatusCode}")
  }

  private def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val xmlWidth = 120
    val xmlIndent = 2

    val statusResponseXml = statusResponseFilterService.transform(xmlResponseBody).head
    val statusResponseString = new PrettyPrinter(xmlWidth, xmlIndent).format(statusResponseXml, TopScope)

    HttpResponse(response.status, None, response.allHeaders, Some(statusResponseString))
  }
}
