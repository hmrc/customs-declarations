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

package uk.gov.hmrc.customs.declaration.connectors

import com.google.inject.*
import org.apache.pekko.actor.ActorSystem
import play.api.http.HeaderNames.*
import play.api.http.{ContentTypes, MimeTypes}
import play.api.mvc.Codec.utf_8
import uk.gov.hmrc.customs.declaration.config.{DeclarationCircuitBreaker, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.{CdsLogger, DeclarationsLogger}
import uk.gov.hmrc.customs.declaration.model.ApiVersion
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpErrorFunctions, HttpException, HttpResponse, StringContextOps}
import play.api.libs.ws.writeableOf_String

import java.time.{Instant, LocalDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.NodeSeq

@Singleton
class DeclarationSubmissionConnector @Inject()(override val http: HttpClientV2,
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
class DeclarationCancellationConnector @Inject()(override val http: HttpClientV2,
                                                 override val logger: DeclarationsLogger,
                                                 override val serviceConfigProvider: ServiceConfigProvider,
                                                 override val config: DeclarationsConfigService,
                                                 override val cdsLogger: CdsLogger,
                                                 override val actorSystem: ActorSystem)
                                                (implicit val ec: ExecutionContext)
  extends DeclarationConnector {

  override val configKey = "declaration-cancellation"
}

trait DeclarationConnector extends DeclarationCircuitBreaker with HttpErrorFunctions with HeaderUtil {

  def http: HttpClientV2

  def logger: DeclarationsLogger

  def serviceConfigProvider: ServiceConfigProvider

  def config: DeclarationsConfigService

  override val numberOfCallsToTriggerStateChange: Int = config.declarationsCircuitBreakerConfig.numberOfCallsToTriggerStateChange
  override val unstablePeriodDurationInMillis: Int = config.declarationsCircuitBreakerConfig.unstablePeriodDurationInMillis
  override val unavailablePeriodDurationInMillis: Int = config.declarationsCircuitBreakerConfig.unavailablePeriodDurationInMillis

  def send[A](xml: NodeSeq, date: Instant, correlationId: UUID, apiVersion: ApiVersion)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))

    val decHeaders = getHeaders(date, correlationId) ++ Seq(HeaderNames.authorisation -> bearerToken) ++ getCustomsApiStubExtraHeaders(hc)
    val startTime = LocalDateTime.now
    withCircuitBreaker(post(xml, config.url, decHeaders)).map {
      response => {
        logCallDuration(startTime)
        logger.debug(s"Response status ${response.status} and response body ${formatResponseBody(response.body)}")
      }
        response
    }
  }

  private def getHeaders(date: Instant, correlationId: UUID) = {
    Seq(
      (ACCEPT, MimeTypes.XML),
      (CONTENT_TYPE, ContentTypes.XML(utf_8)),
      (DATE, getDateHeader(date)),
      (X_FORWARDED_HOST, "MDTP"),
      ("X-Correlation-ID", correlationId.toString))
  }

  private def post[A](xml: NodeSeq, url: String, decHeaders: Seq[(String, String)])(implicit vpr: ValidatedPayloadRequest[A]) = {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    logger.debug(s"Sending request to $url.\n Headers:\n $decHeaders\n Payload:\n$xml")

    http.post(url"$url").setHeader(decHeaders*).withBody(xml.toString).execute[HttpResponse].map { response =>
      response.status match {
        case status if is2xx(status) =>
          response

        case status => //1xx, 3xx, 4xx, 5xx
          throw Non2xxResponseException(s"Call to Declarations backend failed. Status=[$status] url=[$url] response body=[${formatResponseBody(response.body)}]", status)
      }
    }
      .recoverWith {
        case httpError: HttpException =>
          Future.failed(httpError)
        case NonFatal(e) =>
          logger.error(s"Call to declaration submission failed. url=[$url]")
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
