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

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.GenericValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionFieldsResponse, ApiSubscriptionKey}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class ApiSubscriptionFieldsConnector @Inject()(http: HttpClient,
                                               logger: DeclarationsLogger,
                                               config: DeclarationsConfigService) {

  def getSubscriptionFields[A](apiSubsKey: ApiSubscriptionKey)(implicit vpr: GenericValidatedPayloadRequest[A], hc: HeaderCarrier): Future[ApiSubscriptionFieldsResponse] = {
    val url = ApiSubscriptionFieldsPath.url(config.apiSubscriptionFieldsBaseUrl, apiSubsKey)
    get(url)
  }

  private def get[A](url: String)(implicit vpr: GenericValidatedPayloadRequest[A], hc: HeaderCarrier): Future[ApiSubscriptionFieldsResponse] = {
    logger.debug(s"Getting fields id from api subscription fields service. url=$url")

    http.GET[ApiSubscriptionFieldsResponse](url)
      .recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
      }
      .recoverWith {
        case e: Throwable =>
          logger.error(s"Call to subscription information service failed. url=$url")
          Future.failed(e)
      }
  }

}
