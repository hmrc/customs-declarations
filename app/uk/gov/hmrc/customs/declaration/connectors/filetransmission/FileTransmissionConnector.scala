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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions, HttpException, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileTransmissionConnector @Inject()(http: HttpClient,
                                          logger: DeclarationsLogger,
                                          config: DeclarationsConfigService)
                                         (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private implicit val hc: HeaderCarrier = HeaderCarrier(
    extraHeaders = Seq(ACCEPT -> JSON, CONTENT_TYPE -> JSON) // http-verbs will implicitly add user agent header
  )

  def send[A](request: FileTransmission)(implicit hasConversationId: HasConversationId, hc: HeaderCarrier): Future[Unit] = {
    println(Console.YELLOW_B + Console.BLACK + s"SEND - CONNECTOR - HEADER = $hc" + Console.RESET)
    post(request, config.fileUploadConfig.fileTransmissionBaseUrl)(hasConversationId, hc)
  }

  private def post[A](request: FileTransmission, url: String)(implicit hasConversationId: HasConversationId, hc: HeaderCarrier): Future[Unit] = {
    logger.debug(s"Sending request to file transmission service. Url: $url Payload:\n${Json.prettyPrint(Json.toJson(request))}")
    http.POST[FileTransmission, HttpResponse](url, request)(implicitly, implicitly, hc, implicitly).map{ response =>
      response.status match {
        case status if is2xx(status) =>
          logger.info(s"[conversationId=${request.file.reference}]: file transmission request sent successfully")
          println(Console.YELLOW_B + Console.BLACK + s"HEADER = $hc" + Console.RESET)
          ()

        case status => //1xx, 3xx, 4xx, 5xx
          throw Non2xxResponseException(status)
      }
    }.recoverWith {
        case httpError: HttpException =>
          logger.error(s"Call to file transmission failed. url=$url, HttpStatus=${httpError.responseCode}, Error=${httpError.message}")
          println(Console.YELLOW_B + Console.BLACK + s"HEADER fail= $hc" + Console.RESET)
          Future.failed(new RuntimeException(httpError))
        case e: Throwable =>
          println(Console.YELLOW_B + Console.BLACK + s"HEADER error= $hc" + Console.RESET)
          logger.error(s"Call to file transmission failed. url=$url")
          Future.failed(e)
      }
  }
}
