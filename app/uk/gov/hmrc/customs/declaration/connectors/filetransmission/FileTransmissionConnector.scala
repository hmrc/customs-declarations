/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.connectors.filetransmission

import com.google.inject._
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames._
import play.mvc.Http.MimeTypes.JSON
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.filetransmission.FileTransmission
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpException, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileTransmissionConnector @Inject()(http: HttpClient,
                                          logger: DeclarationsLogger,
                                          config: DeclarationsConfigService)
                                         (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private implicit val hc: HeaderCarrier = HeaderCarrier(
    extraHeaders = Seq(ACCEPT -> JSON, CONTENT_TYPE -> JSON) // http-verbs will implicitly add user agent header
  )

  def send[A](request: FileTransmission)(implicit hasConversationId: HasConversationId): Future[Unit] = {
    post(request, config.fileUploadConfig.fileTransmissionBaseUrl)
  }

  private def post[A](request: FileTransmission, url: String)(implicit hasConversationId: HasConversationId): Future[Unit] = {
    logger.debug(s"Sending request to file transmission service. Url: $url Payload:\n${Json.prettyPrint(Json.toJson(request))}")
    http.POST[FileTransmission, HttpResponse](url, request).map{ response =>
      response.status match {
        case status if is2xx(status) =>
          logger.info(s"[conversationId=${request.file.reference}]: file transmission request sent successfully")
          ()

        case status => //1xx, 3xx, 4xx, 5xx
          throw new Non2xxResponseException(status)
      }
    }.recoverWith {
        case httpError: HttpException =>
          logger.error(s"Call to file transmission failed. url=$url, HttpStatus=${httpError.responseCode}, Error=${httpError.message}")
          Future.failed(new RuntimeException(httpError))
        case e: Throwable =>
          logger.error(s"Call to file transmission failed. url=$url")
          Future.failed(e)
      }
  }
}
