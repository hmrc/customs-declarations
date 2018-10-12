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

import java.net.{URL, URLEncoder}
import java.util.UUID
import javax.inject.{Inject, Singleton}

import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, BatchUpscanInitiateConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedBatchFileUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.repo.BatchFileUploadMetadataRepo
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, Text}

@Singleton
class BatchFileUploadBusinessService @Inject()(batchUpscanInitiateConnector: BatchUpscanInitiateConnector,
                                               batchFileUploadMetadataRepo: BatchFileUploadMetadataRepo,
                                               uuidService: UuidService,
                                               logger: DeclarationsLogger,
                                               apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                               config: DeclarationsConfigService) {

  private val apiContextEncoded = URLEncoder.encode("customs/declarations", "UTF-8")
  private case class UpscanInitiateRequest(subscriptionFieldsId: SubscriptionFieldsId, documentType: DocumentType)

  def send[A](implicit validatedRequest: ValidatedBatchFileUploadPayloadRequest[A],
              hc: HeaderCarrier): Future[Either[Result, NodeSeq]] = {

    futureApiSubFieldsId(validatedRequest.clientId).flatMap {
      case Right(sfId) =>
        batchBackendCalls(sfId).flatMap { fileDetails =>
          persist(fileDetails, sfId).map {
            case true =>
              val responseBodyXml = serialize(fileDetails)
              logger.debug(s"response body xml to be returned=${responseBodyXml.toString()}")
              Right(responseBodyXml)
            case false => Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
          }
        }.recover {
          case NonFatal(e) =>
            logger.error(s"Upscan initiate call failed: ${e.getMessage}", e)
            Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
        }
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  private def futureApiSubFieldsId[A](c: ClientId)
                                     (implicit validatedRequest: ValidatedBatchFileUploadPayloadRequest[A],
                                      hc: HeaderCarrier): Future[Either[Result, SubscriptionFieldsId]] = {
    (apiSubFieldsConnector.getSubscriptionFields(ApiSubscriptionKey(c, apiContextEncoded, validatedRequest.requestedApiVersion)) map {
      response: ApiSubscriptionFieldsResponse =>
        Right(SubscriptionFieldsId(response.fieldsId))
    }).recover {
      case NonFatal(e) =>
        logger.error(s"Subscriptions fields lookup call failed: ${e.getMessage}", e)
        Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def batchBackendCalls[A](subscriptionFieldsId: SubscriptionFieldsId)
                            (implicit validatedRequest: ValidatedBatchFileUploadPayloadRequest[A],
                             hc: HeaderCarrier): Future[Seq[UpscanInitiateResponsePayload]] = {

    val upscanInitiateRequests = validatedRequest.batchFileUploadRequest.files.map { uploadProperties =>
      UpscanInitiateRequest(subscriptionFieldsId, uploadProperties.documentType)
    }
    failFastSequence(upscanInitiateRequests)(i => backendCall(i))
  }

  private def persist[A](fileDetails: Seq[UpscanInitiateResponsePayload], sfId: SubscriptionFieldsId)
                        (implicit request: ValidatedBatchFileUploadPayloadRequest[A]): Future[Boolean] = {
    //TODO ensure/check that ordering of uploadProperties matches batchFiles
    val batchFiles = fileDetails.zipWithIndex.map { case (fileDetail, index) =>
      BatchFile(FileReference(UUID.fromString(fileDetail.reference)), None, new URL(fileDetail.uploadRequest.href),
        request.batchFileUploadRequest.files(index).fileSequenceNo, 1, request.batchFileUploadRequest.files(index).documentType)
    }

    val metadata = BatchFileUploadMetadata(request.batchFileUploadRequest.declarationId, extractEori(request.authorisedAs), sfId,
      BatchId(uuidService.uuid()), request.batchFileUploadRequest.fileGroupSize.value, batchFiles)

    batchFileUploadMetadataRepo.create(metadata)
  }

  private def serialize(payloads: Seq[UpscanInitiateResponsePayload]): NodeSeq = {

    <FileUploadResponse xmlns="hmrc:batchfileupload">
      <Files>
        {payloads.map(payload =>
        <File>
          <reference>{payload.reference}</reference>
          <uploadRequest>
            <href>{payload.uploadRequest.href}</href>
            <fields>
              {payload.uploadRequest.fields.map(field =>
            {<a/>.copy(label = field._1, child = Text(field._2))}
            )}
            </fields>
          </uploadRequest>
        </File>
      )}
      </Files>
    </FileUploadResponse>
  }

  private def failFastSequence[A,B](iter: Iterable[A])(fn: A => Future[B]): Future[Seq[B]] =
    iter.foldLeft(Future(Seq.empty[B])) {
      (previousFuture, next) =>
        for {
          previousResults <- previousFuture
          next <- fn(next)
        } yield previousResults :+ next
    }

  private def backendCall[A](upscanInitiateRequest: UpscanInitiateRequest)
                              (implicit validatedRequest: ValidatedBatchFileUploadPayloadRequest[A], hc: HeaderCarrier) = {
    batchUpscanInitiateConnector.send(
      preparePayload(upscanInitiateRequest.subscriptionFieldsId), validatedRequest.requestedApiVersion)
  }

  private def extractEori(authorisedAs: AuthorisedAs): Eori = {
    authorisedAs match {
      case nonCsp: NonCsp => nonCsp.eori
      case batchFileUploadCsp: BatchFileUploadCsp => batchFileUploadCsp.eori
      case _: Csp => throw new IllegalStateException("CSP route must be via BatchFileUploadCsp")
    }
  }

  private def preparePayload[A](subscriptionFieldsId: SubscriptionFieldsId)
                               (implicit validatedRequest: ValidatedBatchFileUploadPayloadRequest[A], hc: HeaderCarrier): UpscanInitiatePayload = {

    val upscanInitiatePayload = UpscanInitiatePayload(
      s"""${config.batchFileUploadConfig.batchFileUploadCallbackUrl}/uploaded-batch-file-upscan-notifications/clientSubscriptionId/${subscriptionFieldsId.value}""".stripMargin)
    logger.debug(s"Prepared payload for upscan initiate $upscanInitiatePayload")
    upscanInitiatePayload
  }

}
