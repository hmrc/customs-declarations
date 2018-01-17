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
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.WSHttp
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class MdgWcoDeclarationConnector @Inject()(http: WSHttp,
                                           logger: DeclarationsLogger,
                                           serviceConfigProvider: ServiceConfigProvider) {

  private val configKey = "wco-declaration"

  def send(xml: NodeSeq, date: DateTime, correlationId: UUID, configKeyPrefix: Option[String] = None): Future[HttpResponse] = {
    val config = Option(serviceConfigProvider.getConfig(getConfigKey(configKeyPrefix))).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = getHeaders(date, correlationId), authorization = Some(Authorization(bearerToken)))
    post(xml, config.url)
  }

  private def getConfigKey(configKeyPrefix: Option[String]) = {
    configKeyPrefix match {
      case Some(prefix) => s"$prefix.$configKey"
      case None => configKey
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

  private def post(xml: NodeSeq, url: String)(implicit hc: HeaderCarrier) = {
  //making this call
    http.POSTString[HttpResponse](url, xml.toString())
      .recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
      }
      .recoverWith {
        case e: Throwable =>
          //log the error here.
          Future.failed(e)
      }
  }
}
