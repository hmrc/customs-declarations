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

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.http.MimeTypes
import play.api.libs.json.Json
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsRequest
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class GoogleAnalyticsConnector @Inject()(http: HttpClient,
                                         logger: DeclarationsLogger,
                                         config: DeclarationsConfigService) {

  private val outboundHeaders = Seq(
    (ACCEPT, MimeTypes.JSON),
    (CONTENT_TYPE, MimeTypes.JSON))

  //TODO MC change (?) Future[HttpResponse] to Future[Unit], since it's supposed to be fire and forget
  def send[A](googleAnalyticsRequest: GoogleAnalyticsRequest)(implicit vpr: ValidatedPayloadRequest[A]): Future[HttpResponse] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = outboundHeaders)
    val msg = "Calling public notification (google analytics) service"
    val url = config.declarationsConfig.googleAnalyticsUrl
    val payloadAsJsonString = Json.prettyPrint(Json.toJson(googleAnalyticsRequest))
    logger.debug(s"$msg at $url with\nheaders=${hc.headers} and\npayload=$payloadAsJsonString googleAnalyticsRequest")

    val postFuture = http
      .POST[GoogleAnalyticsRequest, HttpResponse](url, googleAnalyticsRequest)
      .recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
      }
      .recoverWith {
        case e: Throwable =>
          logger.error(s"Call to public notification service (google analytics) failed. POST url=$url", e)
          Future.failed(e)
      }
    postFuture
  }
}
