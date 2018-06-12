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
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsRequest
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{HasAnalyticsValues, HasConversationId}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class GoogleAnalyticsConnector @Inject()(http: HttpClient,
                                         logger: DeclarationsLogger,
                                         declarationsConfigService: DeclarationsConfigService) {

  private val outboundHeaders = Seq(
    (ACCEPT, MimeTypes.JSON),
    (CONTENT_TYPE, MimeTypes.JSON))

  private val config = declarationsConfigService.googleAnalyticsConfig

  private def payload(eventName: String, eventLabel: String) = {
    s"v=1&t=event&tid=${config.trackingId}&cid=${config.clientId}&ec=CDS&ea=$eventName&el=$eventLabel&ev=${config.eventValue}"
  }


  def success(implicit data: HasConversationId with HasAnalyticsValues): Future[Unit] = {
    send(data.analyticsValues.success, s"ConversationId: ${data.conversationId}")
  }

  def failure(response: ErrorResponse)(implicit data: HasConversationId with HasAnalyticsValues): Future[Unit] = {
    val errorCode = response.httpStatusCode

    if (errorCode > 399 && errorCode < 500) {
      failure(response.message)
    } else {
      Future.successful(())
    }

  }

  def failure(error: String)(implicit data: HasConversationId with HasAnalyticsValues): Future[Unit] = {
    send(data.analyticsValues.failure, s"ConversationId: ${data.conversationId} $error")
  }

  def send[A](eventName: String, eventLabel: String)(implicit hasConversationId: HasConversationId with HasAnalyticsValues): Future[Unit] = {

    val msg = "Calling public notification (google analytics) service"
    val url = config.url
    implicit val hc: HeaderCarrier = new HeaderCarrier
    val request = GoogleAnalyticsRequest(payload(eventName, eventLabel))
    val payloadAsJsonString = Json.prettyPrint(Json.toJson(request))
    logger.debug(s"$msg at $url with\nheaders=${hc.headers} and\npayload=$payloadAsJsonString googleAnalyticsRequest")

    http.POST[GoogleAnalyticsRequest, HttpResponse](url, request, outboundHeaders)
      .map { _ =>
        logger.debug(s"Successfully sent GA event to $url, eventName= $eventName, eventLabel= $eventLabel, trackingId= ${config.trackingId}")
        ()
      }.recover {
      case ex: Throwable =>
        logger.error(s"Call to GoogleAnalytics sender service failed. POST url= ${config.url}, eventName= $eventName, eventLabel= $eventLabel, reason= ${ex.getMessage}")
    }
  }
}
