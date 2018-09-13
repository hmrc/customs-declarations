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

import com.google.inject._
import play.api.libs.json.Json
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, NrSubmissionId, NrsPayload}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NrsConnector @Inject()(http: HttpClient,
                             logger: DeclarationsLogger,
                             serviceConfigProvider: ServiceConfigProvider,
                             declarationConfigService: DeclarationsConfigService) {

  private val configKey = "nrs-service"
  private val XApiKey = "X-API-Key"

  def send[A](nrsPayload: NrsPayload, apiVersion: ApiVersion)(implicit vpr: ValidatedPayloadRequest[A]): Future[NrSubmissionId] = {
    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    post(nrsPayload, config.url)
  }

  private def post[A](payload: NrsPayload, url: String)(implicit vupr: ValidatedPayloadRequest[A]) = {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    logger.debug(s"Sending request to nrs service. Url: $url Payload: ${Json.prettyPrint(Json.toJson(payload))}")

    http.POST[NrsPayload, NrSubmissionId](url, payload, Seq[(String, String)](("Content-Type", "application/json"), (XApiKey, declarationConfigService.nrsConfig.nrsApiKey)))
      .map { res =>
        logger.debug(s"Response received from nrs service $res")
        res
      }
      .recoverWith {
        case httpError: HttpException =>
          logger.error(s"Call to nrs service failed url=$url, HttpException=$httpError")
          Future.failed(new RuntimeException(httpError))
        case e: Throwable =>
          logger.error(s"Call to nrs service failed url=$url, exception=$e")
          Future.failed(e)
      }
  }
}
