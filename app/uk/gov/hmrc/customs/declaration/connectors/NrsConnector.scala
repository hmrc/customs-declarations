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

import java.lang.String.format
import java.math.BigInteger
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest.getInstance

import com.google.common.io.BaseEncoding.base64
import com.google.inject._
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, NrsMetadata, NrsPayload}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NrsConnector @Inject()(http: HttpClient,
                             logger: DeclarationsLogger,
                             serviceConfigProvider: ServiceConfigProvider) {

  private val configKey = "nrs-service"

  private val conversationIdKey = "conversationId"
  private val applicationXml = "application/xml"
  private val businessIdValue = "cds"
  private val notableEventValue = "cds-declaration"

  def send[A](apiVersion: ApiVersion)(implicit vpr: ValidatedPayloadRequest[A]): Future[HttpResponse] = {

    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))

    val nrsMetadata = new NrsMetadata(businessId = businessIdValue,
      notableEvent = notableEventValue,
      payloadContentType = applicationXml,
      payloadSha256Checksum = sha256Hash(vpr.request.body.toString),
      userSubmissionTimestamp = DateTime.now().toString,
      identityData = vpr.nrsRetrievalData,
      headerData = new JsObject(vpr.request.headers.toMap.map(x => x._1 -> JsArray(x._2.map(JsString)))),
      searchKeys = JsObject(Map[String, JsValue](conversationIdKey -> JsString(vpr.conversationId.toString)))
    )

    val nrsPayload: NrsPayload = NrsPayload(base64().encode(vpr.request.body.toString.getBytes(UTF_8)), nrsMetadata)

    post(nrsPayload, config.url)
  }

  private def post[A](payload: NrsPayload, url: String)(implicit vupr: ValidatedPayloadRequest[A]) = {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    logger.debug(s"Sending request to nrs service. Url: $url Payload: ${payload.toString}")

    http.POST[NrsPayload, HttpResponse](url, payload)
      .map { res =>
        logger.debug(s"Response received from nrs service $res")
        res
      }
      .recoverWith {
        case httpError: HttpException => Future.failed(new RuntimeException(httpError))
        case e: Throwable =>
          logger.error(s"Call to nrs service failed. url=$url")
          Future.failed(e)
      }
  }

  private def sha256Hash(text: String) : String =  {
    format("%064x", new BigInteger(1, getInstance("SHA-256").digest(text.getBytes("UTF-8"))))
  }
}
