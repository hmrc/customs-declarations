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

package uk.gov.hmrc.customs.declaration.connectors.upscan

import com.google.inject.{Inject, Singleton}
import play.mvc.Http.HeaderNames.*
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.declaration.connectors.HeaderUtil
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.customs.declaration.services.upscan.FileUploadCustomsNotification
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpException, HttpResponse, StringContextOps}

import scala.util.control.NonFatal
import play.api.libs.ws.writeableOf_String

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadCustomsNotificationConnector @Inject()(http: HttpClientV2,
                                                       logger: CdsLogger,
                                                       config: DeclarationsConfigService)
                                                      (implicit ec: ExecutionContext) extends HttpErrorFunctions with HeaderUtil {

  private val XMLHeader = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""

  def send(notification: FileUploadCustomsNotification)(implicit hc: HeaderCarrier): Future[Unit] = {

    val headers: Seq[(String, String)] = Seq(
      ("X-CDS-Client-ID", notification.clientSubscriptionId.toString),
      ("X-Conversation-ID", notification.conversationId.toString),
      (CONTENT_TYPE, s"${MimeTypes.XML}; charset=UTF-8"),
      (ACCEPT, MimeTypes.XML),
      (AUTHORIZATION, s"Basic ${config.declarationsConfig.customsNotificationBearerToken}")
    ) ++ getCustomsApiStubExtraHeaders

    val url = config.declarationsConfig.customsNotificationBaseBaseUrl

    http.post(url"$url").withBody(notification.payload.toString).execute[HttpResponse].map { response =>
      response.status match {
        case status if is2xx(status) =>
          logger.info(s"[conversationId=${notification.conversationId}][clientSubscriptionId=${notification.clientSubscriptionId}]: notification sent successfully. url=${config.declarationsConfig.customsNotificationBaseBaseUrl}")
          ()

        case status => //1xx, 3xx, 4xx, 5xx
          logger.error(s"Failed file upload notification call response body=${response.body}")
          throw Non2xxResponseException(status)
      }
    }.recoverWith {
      case httpError: HttpException =>
        logger.error(s"Call to customs notification service failed. url=$url, HttpStatus=${httpError.responseCode}, Error=${httpError.message}")
        Future.failed(new RuntimeException(httpError))

      case NonFatal(e) =>
        logger.error(s"[conversationId=${notification.conversationId}][clientSubscriptionId=${notification.clientSubscriptionId}]: Call to customs notification failed. url=${config.declarationsConfig.customsNotificationBaseBaseUrl}")
        Future.failed(e)
    }
  }
}

