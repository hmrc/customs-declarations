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

package uk.gov.hmrc.customs.declaration.connectors

import java.util.UUID

import com.google.inject._
import org.joda.time.DateTime
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE, DATE, X_FORWARDED_HOST}
import play.api.http.MimeTypes
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.declaration.config.DeclarationsCircuitBreaker
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.ApiVersion
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class MdgWcoDeclarationConnector @Inject()(override val http: HttpClient,
                                         override val logger: DeclarationsLogger,
                                         override val serviceConfigProvider: ServiceConfigProvider,
                                         override val config: DeclarationsConfigService)
  extends MdgDeclarationConnector with DeclarationsCircuitBreaker {

  override val configKey = "wco-declaration"
}

@Singleton
class MdgDeclarationCancellationConnector @Inject()(override val http: HttpClient,
                                                    override val logger: DeclarationsLogger,
                                                    override val serviceConfigProvider: ServiceConfigProvider,
                                                    override val config: DeclarationsConfigService)
  extends MdgDeclarationConnector with DeclarationsCircuitBreaker {

  override val configKey = "declaration-cancellation"
}

trait MdgDeclarationConnector extends DeclarationsCircuitBreaker {

  def http: HttpClient

  def logger: DeclarationsLogger

  def serviceConfigProvider: ServiceConfigProvider

  def config: DeclarationsConfigService

  def configKey: String

  def send[A](xml: NodeSeq, date: DateTime, correlationId: UUID, apiVersion: ApiVersion)(implicit vpr: ValidatedPayloadRequest[A]): Future[HttpResponse] = {
    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = getHeaders(date, correlationId), authorization = Some(Authorization(bearerToken)))
    withCircuitBreaker(post(xml, config.url)).map{
      response => logger.debug(s"Response: ${response.body}")
      response
    }
  }

  private def getHeaders(date: DateTime, correlationId: UUID) = {
    Seq(
      (ACCEPT, MimeTypes.XML),
      (CONTENT_TYPE, MimeTypes.XML),
      (DATE, date.toString("EEE, dd MMM yyyy HH:mm:ss z")),
      (X_FORWARDED_HOST, "MDTP"),
      ("X-Correlation-ID", correlationId.toString))
  }

  private def post[A](xml: NodeSeq, url: String)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier) = {
    logger.debug(s"Sending request to $url. Payload: ${xml.toString()}")
    http.POSTString[HttpResponse](url, xml.toString())
      .recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
        case e: Throwable =>
          logger.error(s"Call to wco declaration submission failed. url=$url")
          Future.failed(e)
      }
  }
}
