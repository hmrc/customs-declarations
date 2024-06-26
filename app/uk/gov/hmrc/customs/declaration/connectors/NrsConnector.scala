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

import com.google.inject._
import play.api.libs.json.Json
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, NrSubmissionId, NrsPayload}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsConnector @Inject()(http: HttpClient,
                             logger: DeclarationsLogger,
                             declarationConfigService: DeclarationsConfigService)
                            (implicit ec: ExecutionContext) extends HeaderUtil {

  def send[A](nrsPayload: NrsPayload, apiVersion: ApiVersion)(implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[NrSubmissionId] = {
    post(nrsPayload, declarationConfigService.nrsConfig.nrsUrl)
  }

  private def post[A](payload: NrsPayload, url: String)(implicit vupr: ValidatedPayloadRequest[A], hc: HeaderCarrier) = {

    val nrsHeaders = Seq[(String, String)](("Content-Type", "application/json"), ("X-API-Key", declarationConfigService.nrsConfig.nrsApiKey))
      .++ (getCustomsApiStubExtraHeaders)
    logger.debug(s"Sending request to nrs service. Url: $url Payload:\n${Json.prettyPrint(Json.toJson(payload))}")
    http.POST[NrsPayload, NrSubmissionId](url, payload, nrsHeaders)
      .map { res =>
        logger.debug(s"Response received from nrs service is submission id: ${res}")
        res
      }
      .recoverWith {
        case e: Throwable =>
          logger.error(s"Call to nrs service failed url=$url, exception=$e")
          Future.failed(e)
      }
  }
}
