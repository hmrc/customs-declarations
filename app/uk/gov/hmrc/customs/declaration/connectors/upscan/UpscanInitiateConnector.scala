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

import com.google.inject._
import play.api.libs.json.Json
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedFileUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, UpscanInitiatePayload, UpscanInitiateResponsePayload}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanInitiateConnector @Inject()(http: HttpClient,
                                        logger: DeclarationsLogger,
                                        config: DeclarationsConfigService)
                                       (implicit ec: ExecutionContext) {

  private val headersList = Some(List("Accept", "Gov-Test-Scenario", "X-Correlation-ID"))

  private def apiStubHeaderCarrier()(implicit hc: HeaderCarrier): HeaderCarrier = {
    println(s"================== $hc")
    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++

        // Other headers (i.e Gov-Test-Scenario, Content-Type)
        hc.headers(headersList.getOrElse(Seq.empty))
    )
  }

  def send[A](payload: UpscanInitiatePayload, apiVersion: ApiVersion)(implicit vfupr: ValidatedFileUploadPayloadRequest[A], hc: HeaderCarrier): Future[UpscanInitiateResponsePayload] = {
    val updatedHc = apiStubHeaderCarrier()
    if (payload.isV2) {
      println(Console.YELLOW_B , Console.BLACK + s"Header 1 -> $updatedHc" + Console.RESET)
      post(payload, config.fileUploadConfig.upscanInitiateV2Url)(vfupr, hc = updatedHc)
    } else {
      println(Console.YELLOW_B , Console.BLACK + s"Header 1.2 -> $updatedHc" + Console.RESET)
      post(payload, config.fileUploadConfig.upscanInitiateV1Url)(vfupr, hc = updatedHc)
    }
  }

  private def post[A](payload: UpscanInitiatePayload, url: String)(implicit vfupr: ValidatedFileUploadPayloadRequest[A], hc: HeaderCarrier) = {

    logger.debug(s"Sending request to upscan initiate service. Url: $url Payload:\n${Json.prettyPrint(Json.toJson(payload))}")
    http.POST[UpscanInitiatePayload, UpscanInitiateResponsePayload](url, payload)(implicitly, implicitly, hc, implicitly)
      .map { res: UpscanInitiateResponsePayload =>
        println(Console.YELLOW_B + s"http.POST -> $hc")
        logger.info(s"reference from call to upscan initiate ${res.reference}")
        logger.debug(s"Response received from upscan initiate service $res")
        res
      }
      .recoverWith {
        case httpError: HttpException =>
          println(Console.YELLOW_B + s"http.POST httpError $hc")
          Future.failed(httpError)
        case e: Throwable =>
          println(Console.CYAN_B , Console.BLACK + s"http.POST Error $hc" + Console.RESET)
          logger.error(s"Call to upscan initiate failed.")
          Future.failed(e)
      }
  }
}
