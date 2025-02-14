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

import com.google.inject.*
import play.api.libs.json.Json
import uk.gov.hmrc.customs.declaration.connectors.HeaderUtil
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedFileUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, UpscanInitiatePayload, UpscanInitiateResponsePayload}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, StringContextOps}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanInitiateConnector @Inject()(http: HttpClientV2,
                                        logger: DeclarationsLogger,
                                        config: DeclarationsConfigService)
                                       (implicit ec: ExecutionContext) extends HeaderUtil{

  def send[A](payload: UpscanInitiatePayload, apiVersion: ApiVersion)(implicit vfupr: ValidatedFileUploadPayloadRequest[A], hc: HeaderCarrier): Future[UpscanInitiateResponsePayload] = {
    if (payload.isV2) {
      post(payload, config.fileUploadConfig.upscanInitiateV2Url)
    } else {
      post(payload, config.fileUploadConfig.upscanInitiateV1Url)
    }
  }

  private def post[A](payload: UpscanInitiatePayload, url: String)(implicit vfupr: ValidatedFileUploadPayloadRequest[A], hc: HeaderCarrier) = {

    implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
    val jsonPayload = Json.toJson(payload)
    
    
    logger.debug(s"Sending request to upscan initiate service. Url: $url Payload:\n${Json.prettyPrint(jsonPayload)}")
    http.post(url"$url").withBody(jsonPayload).execute[UpscanInitiateResponsePayload]
      .map { (res: UpscanInitiateResponsePayload) =>
        logger.info(s"reference from call to upscan initiate ${res.reference}")
        logger.debug(s"Response received from upscan initiate service $res")
        res
      }
      .recoverWith {
        case httpError: HttpException =>
          Future.failed(httpError)
        case e: Throwable =>
          logger.error(s"Call to upscan initiate failed.")
          Future.failed(e)
      }
  }
}
