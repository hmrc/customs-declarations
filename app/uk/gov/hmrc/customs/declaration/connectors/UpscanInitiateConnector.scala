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
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, UpscanInitiateResponsePayload, UpscanInitiatePayload}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UpscanInitiateConnector @Inject()(http: HttpClient,
                                        logger: DeclarationsLogger,
                                        serviceConfigProvider: ServiceConfigProvider) {

  private val configKey = "upscan-initiate"

  def send[A](payload: UpscanInitiatePayload, apiVersion: ApiVersion)(implicit vupr: ValidatedUploadPayloadRequest[A]): Future[UpscanInitiateResponsePayload] = {
    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    post(payload, config.url)
  }

  private def post[A](payload: UpscanInitiatePayload, url: String)(implicit vupr: ValidatedUploadPayloadRequest[A]) = {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    logger.debug(s"Sending request to upscan initiate service. Url: $url Payload: ${payload.toString}")
    val eventualResponse = http.POST[UpscanInitiatePayload, UpscanInitiateResponsePayload](url, payload)
    eventualResponse.map(res =>
      res
    ).recoverWith {
      case httpError: HttpException => Future.failed(new RuntimeException(httpError))
      case e: Throwable =>
        logger.error(s"Call to upscan initiate failed. url=$url")
        Future.failed(e)
    }
  }
}
