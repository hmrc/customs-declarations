/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.Inject
import javax.inject.Singleton
import org.apache.commons.lang3.StringUtils.EMPTY
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.NrsPayload
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http.{HttpException, Upstream5xxResponse}
import uk.gov.hmrc.play.audit.EventKeys.{Path, TransactionName}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

@Singleton
class AuditingService @Inject()(logger: DeclarationsLogger,
                                declarationsConfigService: DeclarationsConfigService,
                                auditConnector: AuditConnector)
                               (implicit ec: ExecutionContext) {

  private val auditSourceValue = "customs-declaration-submission"
  private val auditTypeValue = "DeclarationNotificationOutboundCall"
  private val transactionNameValue = "Unsuccessful Non-Repudiation Submission API call"

  private val xRequestIdKey = "x-request-id"
  private val clientIpKey = "clientIP"
  private val clientPortKey = "clientPort"
  private val errorCodeKey = "errorCode"
  private val errorMessageKey = "errorMessage"
  private val businessIdKey = "businessId"
  private val noteableEventKey = "noteableEvent"
  private val xConversationIdKey = "x-conversation-Id"
  private val userSubmissionTimestampKey = "userSubmissionTimestamp"
  private val identityDataKey = "identityData"

  private val xRequestIdHeaderName = "X-Request-Id"
  private val xForwardedForHeaderName = "X-Forwarded-For"
  private val xForwardedPortHeaderName = "X-Forwarded-Port"

  def auditFailedNrs[A](nrsPayload: NrsPayload, upstream5xxResponse: Upstream5xxResponse)(implicit vpr: ValidatedPayloadRequest[A]): Unit = {
    auditFailedNrs(nrsPayload, upstream5xxResponse.upstreamResponseCode, upstream5xxResponse.message)
  }

  def auditFailedNrs[A](nrsPayload: NrsPayload, httpException: HttpException)(implicit vpr: ValidatedPayloadRequest[A]): Unit = {
    auditFailedNrs(nrsPayload, httpException.responseCode, httpException.message)
  }

  private def auditFailedNrs[A](nrsPayload: NrsPayload, responseCode: Int, errorMessage: String)(implicit vpr: ValidatedPayloadRequest[A]): Unit = {

    val tags = Map(
    Path -> declarationsConfigService.nrsConfig.nrsUrl,
    TransactionName -> transactionNameValue,
    xRequestIdKey -> vpr.headers.get(xRequestIdHeaderName).getOrElse(EMPTY),
    clientIpKey -> vpr.headers.get(xForwardedForHeaderName).getOrElse(EMPTY),
    clientPortKey -> vpr.headers.get(xForwardedPortHeaderName).getOrElse(EMPTY))

    val detail = JsObject(Map[String, JsValue](
      errorCodeKey -> JsString(responseCode.toString),
      errorMessageKey -> JsString(errorMessage),
      businessIdKey -> JsString(nrsPayload.metadata.businessId),
      noteableEventKey -> JsString(nrsPayload.metadata.notableEvent),
      xConversationIdKey -> JsString(vpr.conversationId.toString),
      userSubmissionTimestampKey -> JsString(nrsPayload.metadata.userSubmissionTimestamp),
      identityDataKey -> Json.toJson(nrsPayload.metadata.identityData)
    ))

    val extendedDataEvent = ExtendedDataEvent(
      auditSource = auditSourceValue,
      auditType = auditTypeValue,
      tags = tags,
      detail = detail
    )
    auditConnector.sendExtendedEvent(
      extendedDataEvent).onComplete {
      case Success(auditResult) =>
        logger.info("Successfully audited FAILURE event")
        logger.debug(
          s"""Successfully audited FAILURE event with
             |payload=$extendedDataEvent
             |audit response=$auditResult""".stripMargin)
      case Failure(ex) =>
        logger.error("Failed to audit FAILURE event", ex)
    }
  }
}
