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

package uk.gov.hmrc.customs.declaration.connectors

import java.time.LocalDateTime
import java.util.UUID

import akka.actor.ActorSystem
import play.api.mvc.Codec.utf_8
import com.google.inject._
import org.joda.time.DateTime
import play.api.http.HeaderNames._
import play.api.http.{ContentTypes, MimeTypes}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.config.DeclarationCircuitBreaker
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.ApiVersion
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class DeclarationSubmissionConnector @Inject()(override val http: HttpClient,
                                         override val logger: DeclarationsLogger,
                                         override val serviceConfigProvider: ServiceConfigProvider,
                                         override val config: DeclarationsConfigService,
                                         override val cdsLogger: CdsLogger,
                                         override val actorSystem: ActorSystem)
                                        (implicit val ec: ExecutionContext)
  extends DeclarationConnector {

  override val configKey = "wco-declaration"
}

@Singleton
class DeclarationCancellationConnector @Inject()(override val http: HttpClient,
                                                    override val logger: DeclarationsLogger,
                                                    override val serviceConfigProvider: ServiceConfigProvider,
                                                    override val config: DeclarationsConfigService,
                                                    override val cdsLogger: CdsLogger,
                                                    override val actorSystem: ActorSystem)
                                                   (implicit val ec: ExecutionContext)
  extends DeclarationConnector {

  override val configKey = "declaration-cancellation"
}

trait DeclarationConnector extends DeclarationCircuitBreaker with HttpErrorFunctions {

  def http: HttpClient

  def logger: DeclarationsLogger

  def serviceConfigProvider: ServiceConfigProvider

  def config: DeclarationsConfigService

  override lazy val numberOfCallsToTriggerStateChange = config.declarationsCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override lazy val unstablePeriodDurationInMillis = config.declarationsCircuitBreakerConfig.unstablePeriodDurationInMillis
  override lazy val unavailablePeriodDurationInMillis = config.declarationsCircuitBreakerConfig.unavailablePeriodDurationInMillis

  def send[A](xml: NodeSeq, date: DateTime, correlationId: UUID, apiVersion: ApiVersion)(implicit vpr: ValidatedPayloadRequest[A]): Future[HttpResponse] = {
    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    implicit val hc: HeaderCarrier = HeaderCarrier(authorization = None)

    lazy val decHeaders = getHeaders(date, correlationId) ++ Seq(HeaderNames.authorisation -> bearerToken)
    val startTime = LocalDateTime.now
    withCircuitBreaker(post(xml, config.url, decHeaders)).map{
      response => {
        logCallDuration(startTime)
        logger.debug(s"Response status ${response.status} and response body ${formatResponseBody(response.body)}")
      }
      response
    }
  }

  private def getHeaders(date: DateTime, correlationId: UUID) = {
    Seq(
      (ACCEPT, MimeTypes.XML),
      (CONTENT_TYPE,ContentTypes.XML(utf_8)),
      (DATE, date.toString("EEE, dd MMM yyyy HH:mm:ss z")),
      (X_FORWARDED_HOST, "MDTP"),
      ("X-Correlation-ID", correlationId.toString))
  }

  private def post[A](xml: NodeSeq, url: String, decHeaders: Seq[(String, String)])(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier) = {
    logger.debug(s"Sending request to $url.\n Headers:\n $hc\n Payload:\n$xml")

    http.POSTString[HttpResponse](url, xml.toString(), headers = decHeaders).map{ response =>
      response.status match {
        case status if is2xx(status) =>
          response

        case status => //1xx, 3xx, 4xx, 5xx
          logger.error(s"Failed backend call response body=${formatResponseBody(response.body)}")
          throw new Non2xxResponseException(status)
      }
    }
      .recoverWith {
        case httpError: HttpException =>
          logger.error(s"Failed call to url=$url, HttpStatus=${httpError.responseCode}, Error=${httpError.message}")
          Future.failed(httpError)
        case e: Throwable =>
          logger.error(s"Call to declaration submission failed. url=$url")
          Future.failed(e)
      }
  }

  private def formatResponseBody(responseBody: String) = {
    if (responseBody.isEmpty) {
      "<empty>"
    } else {
      responseBody
    }
  }
}
