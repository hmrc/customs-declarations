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

package uk.gov.hmrc.customs.declaration.services

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreakerOpenException
import play.api.http.Status.FORBIDDEN
import play.api.mvc.Result
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, DeclarationCancellationConnector, DeclarationConnector, DeclarationSubmissionConnector}
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse.errorInternalServerError
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}

import java.net.URLEncoder
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZonedDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.NodeSeq

@Singleton
class StandardDeclarationSubmissionService @Inject()(override val logger: DeclarationsLogger,
                                                     override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                                     override val connector: DeclarationSubmissionConnector,
                                                     override val wrapper: MdgPayloadDecorator,
                                                     override val dateTimeProvider: DateTimeService,
                                                     override val uniqueIdsService: UniqueIdsService,
                                                     override val nrsService: NrsService,
                                                     override val declarationsConfigService: DeclarationsConfigService,
                                                     override val actorSystem: ActorSystem)
                                                    (implicit val ec: ExecutionContext) extends DeclarationService

@Singleton
class CancellationDeclarationSubmissionService @Inject()(override val logger: DeclarationsLogger,
                                                         override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                                         override val connector: DeclarationCancellationConnector,
                                                         override val wrapper: MdgPayloadDecorator,
                                                         override val dateTimeProvider: DateTimeService,
                                                         override val uniqueIdsService: UniqueIdsService,
                                                         override val nrsService: NrsService,
                                                         override val declarationsConfigService: DeclarationsConfigService,
                                                         override val actorSystem: ActorSystem)
                                                        (implicit val ec: ExecutionContext) extends DeclarationService {
}

trait DeclarationService extends ApiSubscriptionFieldsService {

  def connector: DeclarationConnector

  def wrapper: MdgPayloadDecorator

  def dateTimeProvider: DateTimeService

  def uniqueIdsService: UniqueIdsService

  def nrsService: NrsService

  def declarationsConfigService: DeclarationsConfigService

  def actorSystem: ActorSystem

  implicit def ec: ExecutionContext

  private val errorResponseServiceUnavailable = errorInternalServerError("This service is currently unavailable")

  def send[A](implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, Option[NrSubmissionId]]] = {
    futureApiSubFieldsId(vpr.clientId) flatMap {
      case Right(sfId) =>
        callBackendAndNrs(vpr, sfId, hc)
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  private def callBackendAndNrs[A](implicit vpr: ValidatedPayloadRequest[A], asfr: ApiSubscriptionFieldsResponse, hc: HeaderCarrier): Future[Either[Result, Option[NrSubmissionId]]] = {

    val nrSubmissionId = new NrSubmissionId(vpr.conversationId.uuid)
    if (declarationsConfigService.nrsConfig.nrsEnabled) {
      logger.debug("NRS enabled. Calling NRS.")

      val startTime = dateTimeProvider.zonedDateTimeUtc

      nrsService.send(vpr, hc)
        .map(nrSubmissionId => {
          logger.debug(s"NRS returned submission id: $nrSubmissionId")
          logCallDuration(startTime)
        })
        .recover {
          case ex: Exception => logger.warn(s"NRS call failed: $ex")
        }
    } else {
      logger.debug("NRS not enabled")
    }

    callBackend(asfr).map {
      case Left(errorResult) =>
        logger.debug("MDG call failed")
        Left(errorResult.withNrSubmissionId(nrSubmissionId))
      case Right(_) =>
        logger.debug("MDG call success.")
        if (declarationsConfigService.nrsConfig.nrsEnabled) Right(Some(nrSubmissionId)) else Right(None)
    }
  }

  private def callBackend[A](asfr: ApiSubscriptionFieldsResponse)
                            (implicit vpr: ValidatedPayloadRequest[A],  hc: HeaderCarrier): Future[Either[Result, Option[NrSubmissionId]]] = {
    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation
    val xmlToSend = preparePayload(vpr.xmlBody, asfr, dateTime)

    def handleResult(t: Throwable, errorResponse: ErrorResponse) = {
      logger.warn(s"Returning status=[${errorResponse.httpStatusCode}]. ${t.getMessage}", t)
      Left(errorResponse.XmlResult.withConversationId)
    }

    connector.send(xmlToSend, dateTime, correlationId.uuid, vpr.requestedApiVersion).map(_ => Right(None)).recover {
      case _: CircuitBreakerOpenException =>
        logger.error("unhealthy state entered")
        Left(errorResponseServiceUnavailable.XmlResult.withConversationId)
      case httpError: HttpException if httpError.responseCode == FORBIDDEN =>
        handleResult(httpError, ErrorResponse.ErrorPayloadForbidden)
      case NonFatal(e) =>
        handleResult(e, ErrorResponse.ErrorInternalServerError)
    }
  }

  private def preparePayload[A](xml: NodeSeq, asfr: ApiSubscriptionFieldsResponse, dateTime: Instant)
                               (implicit vpr: ValidatedPayloadRequest[A]): NodeSeq = {
    logger.debug(s"preparePayload called")
    wrapper.wrap(xml, asfr, dateTime)
  }

  private def logCallDuration[A](startTime: ZonedDateTime)
                                (implicit vpr: ValidatedPayloadRequest[A]): Unit = {
    val endTime = dateTimeProvider.zonedDateTimeUtc
    val callDuration = ChronoUnit.MILLIS.between(startTime, endTime)
    logger.info(s"Duration of call to NRS $callDuration ms")
  }

}

trait ApiSubscriptionFieldsService {

  def apiSubFieldsConnector: ApiSubscriptionFieldsConnector

  def logger: DeclarationsLogger

  implicit def ec: ExecutionContext

  private val apiContextEncoded = URLEncoder.encode("customs/declarations", "UTF-8")

  def futureApiSubFieldsId[A](c: ClientId)
                             (implicit vpr: HasConversationId with HasApiVersion with HasAuthorisedAs with ExtractedHeaders, hc: HeaderCarrier): Future[Either[Result, ApiSubscriptionFieldsResponse]] = {
    (apiSubFieldsConnector.getSubscriptionFields(ApiSubscriptionKey(c, apiContextEncoded, vpr.requestedApiVersion)) map {
      response: ApiSubscriptionFieldsResponse =>
        vpr.authorisedAs match {
          case Csp(_, _, _) =>
            if (response.fields.authenticatedEori.exists(_.trim.nonEmpty)) {
              Right(response)
            } else {
              logger.error("authenticatedEori for CSP not returned from api subscription fields")
              Left(errorInternalServerError("Missing authenticated eori in service lookup").XmlResult.withConversationId)
            }
          case _ =>
            Right(response)
        }
    }).recover {
      case NonFatal(e) =>
        logger.error(s"Subscriptions fields lookup call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

}
