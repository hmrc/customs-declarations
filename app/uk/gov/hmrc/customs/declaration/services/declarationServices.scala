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

import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.mvc.Result
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorInternalServerError
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, MdgDeclarationCancellationConnector, MdgDeclarationConnector, MdgWcoDeclarationConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left
import scala.util.control.NonFatal
import scala.xml.NodeSeq

@Singleton
class StandardDeclarationSubmissionService @Inject()(override val logger: DeclarationsLogger,
                                                     override val connector: MdgWcoDeclarationConnector,
                                                     override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                                     override val wrapper: MdgPayloadDecorator,
                                                     override val dateTimeProvider: DateTimeService,
                                                     override val uniqueIdsService: UniqueIdsService,
                                                     override val nrsService: NrsService,
                                                     override val declarationsConfigService: DeclarationsConfigService,
                                                     override val actorSystem: ActorSystem
                                                    ) extends DeclarationService

@Singleton
class CancellationDeclarationSubmissionService @Inject()(override val logger: DeclarationsLogger,
                                                     override val connector: MdgDeclarationCancellationConnector,
                                                     override val apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                                     override val wrapper: MdgPayloadDecorator,
                                                     override val dateTimeProvider: DateTimeService,
                                                     override val uniqueIdsService: UniqueIdsService,
                                                     override val nrsService: NrsService,
                                                     override val declarationsConfigService: DeclarationsConfigService,
                                                     override val actorSystem: ActorSystem) extends DeclarationService {
}
trait DeclarationService {

  def logger: DeclarationsLogger

  def connector: MdgDeclarationConnector

  def apiSubFieldsConnector: ApiSubscriptionFieldsConnector

  def wrapper: MdgPayloadDecorator

  def dateTimeProvider: DateTimeService

  def uniqueIdsService: UniqueIdsService

  def nrsService: NrsService

  def declarationsConfigService: DeclarationsConfigService

  def actorSystem: ActorSystem

  private val apiContextEncoded = URLEncoder.encode("customs/declarations", "UTF-8")
  private val errorResponseServiceUnavailable = errorInternalServerError("This service is currently unavailable")

  def send[A](implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, Option[NrSubmissionId]]] = {
    futureApiSubFieldsId(vpr.clientId) flatMap {
      case Right(sfId) =>
          callBackendAndNrs(vpr, hc, sfId)
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  private def callBackendAndNrs[A](implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier, asfr: ApiSubscriptionFieldsResponse): Future[Either[Result, Option[NrSubmissionId]]] = {

  val nrSubmissionId = new NrSubmissionId(vpr.conversationId.uuid)
    if (declarationsConfigService.nrsConfig.nrsEnabled) {
      logger.debug("NRS enabled. Calling NRS.")

      val startTime = dateTimeProvider.zonedDateTimeUtc

      nrsService.send(vpr, hc)
        .map(nrSubmissionId => {
          logger.debug(s"NRS returned submission id: $nrSubmissionId")
          logCallDuration(startTime)
        })
        .recover{
          case ex:Exception => logger.warn(s"NRS call failed: $ex")
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

  private def futureApiSubFieldsId[A](c: ClientId)
                                     (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, ApiSubscriptionFieldsResponse]] = {
    (apiSubFieldsConnector.getSubscriptionFields(ApiSubscriptionKey(c, apiContextEncoded, vpr.requestedApiVersion)) map {
      response: ApiSubscriptionFieldsResponse =>
        vpr.authorisedAs match {
          case Csp(_, _) | CspWithEori(_, _, _) =>
            if (response.fields.authenticatedEori.exists(_.trim.nonEmpty)) {
              Right(response)
            } else {
              logger.error(s"authenticatedEori for CSP not returned from api subscription fields for client id: ${c.value}")
              Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
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

  private def callBackend[A](asfr: ApiSubscriptionFieldsResponse)
                            (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, Option[NrSubmissionId]]] = {
    val dateTime = dateTimeProvider.nowUtc()
    val correlationId = uniqueIdsService.correlation
    val xmlToSend = preparePayload(vpr.xmlBody, asfr, dateTime)

    connector.send(xmlToSend, dateTime, correlationId.uuid, vpr.requestedApiVersion).map(_ => Right(None)).recover {
      case _: UnhealthyServiceException =>
        logger.error("unhealthy state entered")
        Left(errorResponseServiceUnavailable.XmlResult)
      case NonFatal(e) =>
        logger.error(s"submission declaration call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def preparePayload[A](xml: NodeSeq, asfr: ApiSubscriptionFieldsResponse, dateTime: DateTime)
                               (implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): NodeSeq = {
    logger.debug(s"preparePayload called")
    wrapper.wrap(xml, asfr, dateTime)
  }

  private def logCallDuration[A](startTime: ZonedDateTime)
                                  (implicit hc: HeaderCarrier, vpr: ValidatedPayloadRequest[A]): Unit ={
    val endTime = dateTimeProvider.zonedDateTimeUtc
    val callDuration = ChronoUnit.MILLIS.between(startTime, endTime)
    logger.info(s"Duration of call to NRS $callDuration ms")
  }

}
