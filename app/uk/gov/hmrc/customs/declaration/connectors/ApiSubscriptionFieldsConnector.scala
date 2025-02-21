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

import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionFieldsResponse, ApiSubscriptionKey}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, StringContextOps, UpstreamErrorResponse}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal


@Singleton
class ApiSubscriptionFieldsConnector @Inject()(http: HttpClientV2,
                                               logger: DeclarationsLogger,
                                               config: DeclarationsConfigService)
                                              (implicit ec: ExecutionContext) {

  def getSubscriptionFields[A](apiSubsKey: ApiSubscriptionKey)(implicit hci: HasConversationId, hc: HeaderCarrier): Future[ApiSubscriptionFieldsResponse] = {
    val url = ApiSubscriptionFieldsPath.url(config.declarationsConfig.apiSubscriptionFieldsBaseUrl, apiSubsKey)
    get(url)
  }

  private def get[A](urlString: String)(implicit hci: HasConversationId, hc: HeaderCarrier): Future[ApiSubscriptionFieldsResponse] = {
    logger.debug(s"Getting fields id from api subscription fields service. url=$urlString")
    http.get(url"$urlString").execute
      .recoverWith {
        case upstreamError: UpstreamErrorResponse =>
          logger.error(s"Subscriptions fields lookup call failed. url=$urlString HttpStatus=${upstreamError.statusCode} error=${upstreamError.getMessage}")
          Future.failed(upstreamError)

        case httpError: HttpException =>
          logger.error(s"Subscriptions fields lookup call failed. url=$urlString HttpStatus=${httpError.responseCode} error=${httpError.getMessage}")
          Future.failed(new RuntimeException(httpError))

        case NonFatal(e) =>
          logger.error(s"Call to subscription information service failed. url=$urlString")
          Future.failed(e)
      }
  }
}
