/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.connectors.dec64

import play.api.http.{ContentTypes, HeaderNames, MimeTypes, Status}
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import uk.gov.hmrc.customs.declaration.config.ServiceConfiguration
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.model.transmission.TransmissionRequest
import uk.gov.hmrc.customs.declaration.xml.MdgRequestSerialiser
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

sealed trait MdgRequestResult {
  def error: Option[String]
}

case object MdgRequestSuccessful extends MdgRequestResult {
  override def error: Option[String] = None
}

case class MdgRequestError(e: String) extends MdgRequestResult {
  override def error: Option[String] = Some(e)
}

case class MdgRequestFatalError(e: String) extends MdgRequestResult {
  override def error: Option[String] = Some(e)
}

@Singleton
class MdgConnector @Inject()(
                              httpClient: HttpClientV2,
                              serviceConfiguration: ServiceConfiguration,
                              requestSerialiser: MdgRequestSerialiser,
                              logger: CdsLogger
                            )(using ExecutionContext) {

  def requestTransmission(
                           request: TransmissionRequest
                         )(using HeaderCarrier): Future[MdgRequestResult] = {

    val serialisedRequest: String = requestSerialiser.serialise(request)
    val correlationId = generateCorrelationId()
    val headers = buildHeaders(correlationId)

    val safeHeaders = headers.filterNot(_._1 == HeaderNames.AUTHORIZATION)
    logger.debug(
      s"Sent request to MDG [${serviceConfiguration.mdgEndpoint}] with body [$serialisedRequest], headers [$safeHeaders]"
    )

    httpClient
      .post(url"${serviceConfiguration.mdgEndpoint}")
      .setHeader(headers*)
      .withBody(serialisedRequest)
      .execute[HttpResponse]
      .map { response =>
        response.status match
          case s if Status.isSuccessful(s) =>
            logger.info(
              s"Sending request for file with reference [${request.file.reference}] was successful. MDG Correlation id [$correlationId]"
            )
            MdgRequestSuccessful

          case s if Status.isClientError(s) =>
            logger.warn(
              s"Sending request for file with reference [${request.file.reference}] failed. MDG Correlation id [$correlationId]. Cause [${response.body}]"
            )
            MdgRequestFatalError(s"POST of '${serviceConfiguration.mdgEndpoint}' returned status $s. Response body: '${response.body}'")

          case s =>
            logger.warn(
              s"Sending request for file with reference [${request.file.reference}] failed. MDG Correlation id [$correlationId]. Cause [${response.body}]"
            )
            MdgRequestError(s"POST of '${serviceConfiguration.mdgEndpoint}' returned status $s. Response body: '${response.body}'")
      }
      .recover {
        // timeouts and connection errors are retryable
        case NonFatal(e) =>
          logger.warn(
            s"Sending request for file with reference [${request.file.reference}] failed. MDG Correlation id [$correlationId]. Cause [${e.getMessage}]"
          )
          MdgRequestError(s"POST of '${serviceConfiguration.mdgEndpoint}' failed. Cause: '${e.getMessage}'")
      }
  }

  private def generateCorrelationId(): String =
    UUID.randomUUID().toString

  private def buildHeaders(correlationId: String) =
    Seq(
      HeaderNames.CONTENT_TYPE -> ContentTypes.XML,
      HeaderNames.ACCEPT -> MimeTypes.XML,
      HeaderNames.AUTHORIZATION -> s"Bearer ${serviceConfiguration.mdgAuthorizationToken}",
      "X-Correlation-ID" -> correlationId
    )
}
