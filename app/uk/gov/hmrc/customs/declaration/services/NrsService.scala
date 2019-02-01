/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.services

import java.lang.String.format
import java.math.BigInteger
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest.getInstance

import javax.inject.{Inject, Singleton}
import com.google.common.io.BaseEncoding.base64
import play.api.libs.json.{JsObject, JsString, JsValue}
import uk.gov.hmrc.customs.declaration.connectors.NrsConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames.Authorization
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, Upstream5xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NrsService @Inject()(logger: DeclarationsLogger,
                           nrsConnector: NrsConnector,
                           auditingService: AuditingService,
                           dateTimeService: DateTimeService) {

  private val conversationIdKey = "conversationId"
  private val applicationXml = "application/xml"
  private val businessIdValue = "cds"
  private val notableEventValue = "cds-declaration"

  def send[A](implicit vpr: ValidatedPayloadRequest[A], hc: HeaderCarrier): Future[NrSubmissionId] = {

    val nrsMetadata = new NrsMetadata(businessId = businessIdValue,
      notableEvent = notableEventValue,
      payloadContentType = applicationXml,
      payloadSha256Checksum = sha256Hash(vpr.request.body.toString), // This should come from the end user NOT us
      userSubmissionTimestamp = dateTimeService.nowUtc().toString,
      userAuthToken = vpr.headers.get(Authorization).getOrElse(""),
      identityData = vpr.authorisedAs.retrievalData.get, // this should always be populated when nrs is enabled and called
      headerData = new JsObject(vpr.request.headers.toMap.map(x => x._1 -> JsString(x._2 mkString ","))),
      searchKeys = JsObject(Map[String, JsValue](conversationIdKey -> JsString(vpr.conversationId.toString))),
      nrsSubmissionId = vpr.conversationId.toString
    )

    val nrsPayload: NrsPayload = NrsPayload(base64().encode(vpr.request.body.toString.getBytes(UTF_8)), nrsMetadata)

    nrsConnector.send(nrsPayload, vpr.requestedApiVersion).recoverWith {
        case e: HttpException =>
          logger.info(s"Error occurred while submitting NRS payload got HttpException status: ${e.responseCode} error message: ${e.message}")
          if (is5xx(e.responseCode)) {
            auditingService.auditFailedNrs(nrsPayload, e)
          }
          Future.failed(e)
        case e: Upstream5xxResponse =>
          logger.info(s"Error occurred while submitting NRS payload got Upstream5xxResponse status: ${e.upstreamResponseCode} error message: ${e.message}")
          if (is5xx(e.upstreamResponseCode)) {
            auditingService.auditFailedNrs(nrsPayload, e)
          }
          Future.failed(e)
      }
  }

  private def is5xx(statusCode: Int): Boolean ={
    statusCode >= 500 & statusCode < 600
  }

  private def sha256Hash(text: String) : String =  {
    format("%064x", new BigInteger(1, getInstance("SHA-256").digest(text.getBytes("UTF-8"))))
  }
}
