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

import play.api.libs.json.Json
import play.mvc.Http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.mvc.Http.MimeTypes.JSON
import uk.gov.hmrc.customs.declaration.http.NoAuditHttpClient
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.CustomsDeclarationsMetricsRequest
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpException, HttpResponse, StringContextOps}
import scala.util.control.NonFatal

import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDeclarationsMetricsConnector @Inject() (http: NoAuditHttpClient,
                                                     logger: DeclarationsLogger,
                                                     config: DeclarationsConfigService)
                                                    (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private implicit val hc: HeaderCarrier = HeaderCarrier(
    extraHeaders = Seq(ACCEPT -> JSON, CONTENT_TYPE -> JSON)
  )

  def post[A](request: CustomsDeclarationsMetricsRequest)(implicit hasConversationId: HasConversationId): Future[Unit] = {
    post(request, config.declarationsConfig.customsDeclarationsMetricsBaseBaseUrl)
  }

  private def post[A](request: CustomsDeclarationsMetricsRequest, urlString: String)(implicit hasConversationId: HasConversationId): Future[Unit] = {

    logger.debug(s"Sending request to customs declarations metrics service. Url: $urlString Payload:\n${request.toString}")
    http
      .post(url"$urlString")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case status if is2xx(status) =>
            println(s"[conversationId=${request.conversationId}]: customs declarations metrics sent successfully")
            logger.debug(s"[conversationId=${request.conversationId}]: customs declarations metrics sent successfully")

          case status => //1xx, 3xx, 4xx, 5xx
            println(s"Call to customs declarations metrics service failed. url=[$urlString], HttpStatus=[$status], Error=Received a non 2XX response, response body=[${response.body}]")
            logger.error(s"Call to customs declarations metrics service failed. url=[$urlString], HttpStatus=[$status], Error=Received a non 2XX response, response body=[${response.body}]")
        }
        ()
      }.recoverWith {
        case httpError: HttpException =>
          println(s"Call to customs declarations metrics service failed. url=[$urlString], HttpStatus=[${httpError.responseCode}], Error=[${httpError.message}]")
          logger.error(s"Call to customs declarations metrics service failed. url=[$urlString], HttpStatus=[${httpError.responseCode}], Error=[${httpError.message}]")
          Future.failed(new RuntimeException(httpError))
        case NonFatal(e) =>
          println(s"Call to customs declarations metrics service failed. url=[$urlString]")
          logger.warn(s"Call to customs declarations metrics service failed. url=[$urlString]")
          Future.failed(e)
      }
  }
}
