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

import java.net.{URL, URLEncoder}
import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, UpscanInitiateConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedFileUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left
import scala.util.control.NonFatal
import scala.xml._

@Singleton
class FileUploadBusinessService @Inject()(upscanInitiateConnector: UpscanInitiateConnector,
                                          fileUploadMetadataRepo: FileUploadMetadataRepo,
                                          uuidService: UuidService,
                                          logger: DeclarationsLogger,
                                          apiSubFieldsConnector: ApiSubscriptionFieldsConnector,
                                          config: DeclarationsConfigService) {

  private val apiContextEncoded = URLEncoder.encode("customs/declarations", "UTF-8")

  def send[A](implicit validatedRequest: ValidatedFileUploadPayloadRequest[A],
              hc: HeaderCarrier): Future[Either[Result, String]] = {

    futureApiSubFieldsId(validatedRequest.clientId).flatMap {
      case Right(sfId) =>
        backendCalls(sfId).flatMap { fileDetails =>
          persist(fileDetails, sfId).map {
            case true =>
              val responseBody = serialize(fileDetails)
              logger.debug(s"response body to be returned=$responseBody")
              Right(responseBody)
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
                                     (implicit validatedRequest: ValidatedFileUploadPayloadRequest[A],
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

  private def backendCalls[A](subscriptionFieldsId: SubscriptionFieldsId)
                            (implicit validatedRequest: ValidatedFileUploadPayloadRequest[A],
                             hc: HeaderCarrier): Future[Seq[UpscanInitiateResponsePayload]] = {

    val upscanInitiateRequests = validatedRequest.fileUploadRequest.files.map { _ =>
      subscriptionFieldsId
    }
    failFastSequence(upscanInitiateRequests)(i => backendCall(i))
  }

  private def persist[A](fileDetails: Seq[UpscanInitiateResponsePayload], sfId: SubscriptionFieldsId)
                        (implicit request: ValidatedFileUploadPayloadRequest[A]): Future[Boolean] = {
    //TODO ensure/check that ordering of uploadProperties matches batchFiles
    val batchFiles = fileDetails.zipWithIndex.map { case (fileDetail, index) =>
      BatchFile(FileReference(UUID.fromString(fileDetail.reference)), None, new URL(fileDetail.uploadRequest.href),
        request.fileUploadRequest.files(index).fileSequenceNo, 1, request.fileUploadRequest.files(index).maybeDocumentType)
    }

    val metadata = FileUploadMetadata(request.fileUploadRequest.declarationId, extractEori(request.authorisedAs), sfId,
      BatchId(uuidService.uuid()), request.fileUploadRequest.fileGroupSize.value, batchFiles)

    fileUploadMetadataRepo.create(metadata)
  }

  private def serialize(payloads: Seq[UpscanInitiateResponsePayload]): String = {
    prettyPrint(
      <FileUploadResponse xmlns="hmrc:fileupload">
        <Files>
          {payloads.map(payload =>
          <File>
            <Reference>{payload.reference}</Reference>
            <UploadRequest>
              <Href>{payload.uploadRequest.href}</Href>
              <Fields>
                {toNode("Content-Type", payload.uploadRequest.fields)}
                {toNode("acl", payload.uploadRequest.fields)}
                {toNode("key", payload.uploadRequest.fields)}
                {toNode("policy", payload.uploadRequest.fields)}
                {toNode("x-amz-algorithm", payload.uploadRequest.fields)}
                {toNode("x-amz-credential", payload.uploadRequest.fields)}
                {toNode("x-amz-date", payload.uploadRequest.fields)}
                {toNode("x-amz-meta-callback-url", payload.uploadRequest.fields)}
                {toNode("x-amz-signature", payload.uploadRequest.fields)}
              </Fields>
            </UploadRequest>
          </File>
        )}
        </Files>
      </FileUploadResponse>
    )
  }

  private def prettyPrint(xml: Node): String = {
    val xmlWidth = 120
    val xmlIndent = 2

    new PrettyPrinter(xmlWidth, xmlIndent).format(xml)
  }

  private def toNode(labelName: String, fields: Map[String, String]): NodeSeq = {
    if (fields.contains(labelName) && !fields(labelName).trim.isEmpty) {
      <a/>.copy(label = labelName, child = Text(fields(labelName)))
    } else {
      NodeSeq.Empty
    }
  }

  private def failFastSequence[A,B](iter: Iterable[A])(fn: A => Future[B]): Future[Seq[B]] =
    iter.foldLeft(Future(Seq.empty[B])) {
      (previousFuture, next) =>
        for {
          previousResults <- previousFuture
          next <- fn(next)
        } yield previousResults :+ next
    }

  private def backendCall[A](subscriptionFieldsId: SubscriptionFieldsId)
                              (implicit validatedRequest: ValidatedFileUploadPayloadRequest[A], hc: HeaderCarrier) = {
    upscanInitiateConnector.send(
      preparePayload(subscriptionFieldsId), validatedRequest.requestedApiVersion)
  }

  private def extractEori(authorisedAs: AuthorisedAs): Eori = {
    authorisedAs match {
      case nonCsp: NonCsp => nonCsp.eori
      case fileUploadCsp: CspWithEori => fileUploadCsp.eori
      case _: Csp => throw new IllegalStateException("CSP route must be via FileUploadCsp")
    }
  }

  private def preparePayload[A](subscriptionFieldsId: SubscriptionFieldsId)
                               (implicit validatedRequest: ValidatedFileUploadPayloadRequest[A], hc: HeaderCarrier): UpscanInitiatePayload = {

    val upscanInitiatePayload = UpscanInitiatePayload(
      s"""${config.fileUploadConfig.fileUploadCallbackUrl}/uploaded-file-upscan-notifications/clientSubscriptionId/${subscriptionFieldsId.value}""".stripMargin)
    logger.debug(s"Prepared payload for upscan initiate $upscanInitiatePayload")
    upscanInitiatePayload
  }

}
