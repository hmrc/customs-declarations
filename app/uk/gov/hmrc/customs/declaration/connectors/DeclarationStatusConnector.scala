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
import org.joda.time.DateTime
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UsingCircuitBreaker}
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedStatusRequest}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class DeclarationStatusConnector @Inject() (val http: HttpClient,
  val logger: DeclarationsLogger,
  val serviceConfigProvider: ServiceConfigProvider,
  val config: DeclarationsConfigService) extends UsingCircuitBreaker {

  private val configKey = "declaration-status"

  def send[A](date: DateTime,
              correlationId: CorrelationId,
              dmirId: DeclarationManagementInformationRequestId,
              apiVersion: ApiVersion,
              mrn: Mrn)(implicit asr: AuthorisedStatusRequest[A]): Future[HttpResponse] = {

    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = getHeaders(date, asr.conversationId, correlationId), authorization = Some(Authorization(bearerToken)))

    val declarationStatusPayload =
    <n1:queryDeclarationInformationRequest
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/"
      xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns_1="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/"
      xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 request_schema.xsd">
      <n1:requestCommon>
        <n1:clientID>{asr.clientId.toString}</n1:clientID>
        <n1:conversationID>{asr.conversationId.toString}</n1:conversationID>
        <n1:correlationID>{correlationId.toString}</n1:correlationID>
        <n1:badgeIdentifier>{asr.badgeIdentifier.toString}</n1:badgeIdentifier>
        <n1:dateTimeStamp>{date.toString}</n1:dateTimeStamp>
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:declarationManagementInformationRequest>
          <tns_1:id>{dmirId.toString}</tns_1:id>
          <tns_1:timeStamp>{date.toString}</tns_1:timeStamp>
          <xsd_1:reference>{mrn.toString}</xsd_1:reference>
        </n1:declarationManagementInformationRequest>
      </n1:requestDetail>
    </n1:queryDeclarationInformationRequest>

    withCircuitBreaker(post(declarationStatusPayload, config.url)).map{
      response => logger.debug(s"Declaration status response: ${response.body}")
      response
    }
  }

  private def getHeaders(date: DateTime, conversationId: ConversationId, correlationId: CorrelationId) = {
    Seq(
      (X_FORWARDED_HOST, "MDTP"),
        ("X-Correlation-ID", correlationId.toString),
        ("X-Conversation-ID", conversationId.toString),
        (DATE, date.toString("EEE, dd MMM yyyy HH:mm:ss z")),
        (CONTENT_TYPE, MimeTypes.XML),
        (ACCEPT, MimeTypes.XML)
    )
  }

  private def post[A](xml: NodeSeq, url: String)(implicit asr: AuthorisedStatusRequest[A], hc: HeaderCarrier) = {
    logger.debug(s"Sending request to $url. Payload: ${xml.toString()}")
    http.POSTString[HttpResponse](url, xml.toString())
      .recoverWith {
        case httpError: HttpException =>
          logger.error(s"Call to declaration status failed. url=$url")
          Future.failed(new RuntimeException(httpError))
        case e: Throwable =>
          logger.error(s"Call to declaration status failed. url=$url")
          Future.failed(e)
      }
  }

  override protected def circuitBreakerConfig: CircuitBreakerConfig =
    CircuitBreakerConfig(
      serviceName = configKey,
      numberOfCallsToTriggerStateChange = config.declarationsCircuitBreakerConfig.numberOfCallsToTriggerStateChange,
      unavailablePeriodDuration = config.declarationsCircuitBreakerConfig.unavailablePeriodDurationInMillis,
      unstablePeriodDuration = config.declarationsCircuitBreakerConfig.unstablePeriodDurationInMillis
    )

  override protected def breakOnException(t: Throwable): Boolean = t match {
    case _: BadRequestException | _: NotFoundException | _: Upstream4xxResponse => false
    case _ => true
  }
}
