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

package uk.gov.hmrc.customs.declaration.services

import java.net.URLEncoder

import javax.inject.{Inject, Singleton}
import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, UpscanInitiateConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{GenericValidatedPayloadRequest, ValidatedUploadPayloadRequest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left
import scala.util.control.NonFatal

@Singleton
class FileUploadBusinessService @Inject()(logger: DeclarationsLogger,
                                          apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                          upscanInitiateconnector: UpscanInitiateConnector,
                                          config: DeclarationsConfigService) {

  private val apiContextEncoded = URLEncoder.encode("customs/declarations", "UTF-8")

  def send[A](implicit vupr: ValidatedUploadPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, UpscanInitiateResponsePayload]] = {

    futureApiSubFieldsId(vupr.clientId) flatMap {
      case Right(sfId) =>
        callBackend(sfId)
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  private def futureApiSubFieldsId[A](c: ClientId)
                                     (implicit vupr: GenericValidatedPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, SubscriptionFieldsId]] = {
    (apiSubFieldsConnector.getSubscriptionFields(ApiSubscriptionKey(c, apiContextEncoded, vupr.requestedApiVersion)) map {
      response: ApiSubscriptionFieldsResponse =>
        Right(SubscriptionFieldsId(response.fieldsId))
    }).recover {
      case NonFatal(e) =>
        logger.error(s"Subscriptions fields lookup call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def callBackend[A](subscriptionFieldsId: SubscriptionFieldsId)
                            (implicit vupr: ValidatedUploadPayloadRequest[A], hc: HeaderCarrier): Future[Either[Result, UpscanInitiateResponsePayload]] = {
    upscanInitiateconnector.send(preparePayload(subscriptionFieldsId), vupr.requestedApiVersion).map(f => Right(f)).recover{
      case NonFatal(e) =>
        logger.error(s"Upscan initiate call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def preparePayload[A](subscriptionFieldsId: SubscriptionFieldsId)(implicit vupr: ValidatedUploadPayloadRequest[A], hc: HeaderCarrier): UpscanInitiatePayload = {
    val upscanInitiatePayload = UpscanInitiatePayload(s"${config.batchFileUploadConfig.upscanCallbackUrl}/uploaded-file-upscan-notifications/decId/${vupr.declarationId.value}/eori/${vupr.authorisedAs.asInstanceOf[NonCsp].eori.value}/documentationType/${vupr.documentationType.value}/clientSubscriptionId/${subscriptionFieldsId.value}")
    logger.debug(s"Prepared payload for upscan initiate $upscanInitiatePayload")
    upscanInitiatePayload
  }
}
