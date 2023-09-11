/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.http.Status.NOT_FOUND

import javax.inject.{Inject, Singleton}
import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorGenericBadRequest
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, DeclarationStatusConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.{Elem, XML}

@Singleton
class DeclarationStatusService @Inject()(override val logger: DeclarationsLogger,
                                         override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                         connector: DeclarationStatusConnector,
                                         wrapper: MdgPayloadDecorator,
                                         dateTimeProvider: DateTimeService,
                                         uniqueIdsService: UniqueIdsService,
                                         statusResponseFilterService: StatusResponseFilterService,
                                         statusResponseValidationService: StatusResponseValidationService)
                                        (implicit val ec: ExecutionContext) extends ApiSubscriptionFieldsService {

  def send[A](mrn: Mrn)(implicit ar: AuthorisedRequest[A], hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {

    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation
    val dmirId = uniqueIdsService.dmir

    futureApiSubFieldsId(ar.clientId) flatMap {
      case Right(sfId) =>
        val declarationStatusPayload = wrapper.status(correlationId, dateTime, mrn, dmirId, sfId)
        connector.send(declarationStatusPayload, dateTime, correlationId, ar.requestedApiVersion)
          .map(response => validateStatusResponse(ar, response)).recover(recoverException(ar))
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  private def validateStatusResponse[A](implicit ar: AuthorisedRequest[A], response: HttpResponse): Either[Result, HttpResponse] = {
    val xmlResponseBody = XML.loadString(response.body)
    statusResponseValidationService.validate(xmlResponseBody, ar.authorisedAs.asInstanceOf[Csp].badgeIdentifier.get) match {
      case Right(_) => Right(filterResponse(response, xmlResponseBody))
      case Left(errorResponse) =>
        logError(errorResponse)
        Left(errorResponse.XmlResult.withConversationId)
      case _ =>
        logError(ErrorGenericBadRequest)
        Left(ErrorGenericBadRequest.XmlResult.withConversationId)
    }
  }

  private def recoverException[A](implicit ar: AuthorisedRequest[A]): PartialFunction[Throwable, Left[Result, Nothing]] = {
    case e: HttpException if e.responseCode == NOT_FOUND =>
      logger.warn(s"declaration status call failed with 404: ${e.getMessage}")
      Left(ErrorResponse.ErrorNotFound.XmlResult.withConversationId)
    case NonFatal(e) =>
      logger.error(s"declaration status call failed: ${e.getMessage}", e)
      Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
  }

  private def logError[A](errorResponse: ErrorResponse)(implicit ar: AuthorisedRequest[A]): Unit = {
    logger.error(s"declaration status call returning error response '${errorResponse.message}' and status code ${errorResponse.httpStatusCode}")
  }

  private def filterResponse(response: HttpResponse, xmlResponseBody: Elem): HttpResponse = {
    val statusResponseXml = statusResponseFilterService.transform(xmlResponseBody).head
    HttpResponse(response.status, statusResponseXml.toString(), response.headers)
  }
}
