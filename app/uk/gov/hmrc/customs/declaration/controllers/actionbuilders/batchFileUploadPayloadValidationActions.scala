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

package uk.gov.hmrc.customs.declaration.controllers.actionbuilders

import javax.inject.{Inject, Singleton}

import play.api.http.Status
import play.api.mvc.{ActionRefiner, Result}
import play.mvc.Http.Status.FORBIDDEN
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, HttpStatusCodeShortDescriptions, ResponseContents}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.model.{FileGroupSize, _}
import uk.gov.hmrc.customs.declaration.services.{BatchFileUploadXmlValidationService, DeclarationsConfigService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BatchFileUploadPayloadValidationAction @Inject()(batchFileUploadXmlValidationService: BatchFileUploadXmlValidationService,
                                                       logger: DeclarationsLogger,
                                                       googleAnalyticsConnector: GoogleAnalyticsConnector)
    extends PayloadValidationAction(batchFileUploadXmlValidationService, logger, Some(googleAnalyticsConnector))

class BatchFileUploadPayloadValidationComposedAction @Inject()(val batchFileUploadPayloadValidationAction: BatchFileUploadPayloadValidationAction,
                                                               val logger: DeclarationsLogger,
                                                               val declarationsConfigService: DeclarationsConfigService)
  extends ActionRefiner[AuthorisedRequest, ValidatedBatchFileUploadPayloadRequest] with HttpStatusCodeShortDescriptions {

  private val declarationIdLabel = "DeclarationID"
  private val documentTypeLabel = "DocumentType"
  private val fileGroupSizeLabel = "FileGroupSize"
  private val fileSequenceNoLabel = "FileSequenceNo"
  private val filesLabel = "Files"
  private val fileLabel = "File"

  private val errorMaxFileGroupSizeMsg = s"$fileGroupSizeLabel exceeds ${declarationsConfigService.batchFileUploadConfig.fileGroupSizeMaximum} limit"
  private val errorFileGroupSizeMsg = s"$fileGroupSizeLabel does not match number of $fileLabel elements"
  private val errorMaxFileSequenceNoMsg = s"$fileSequenceNoLabel must not be greater than or equal to $fileGroupSizeLabel"
  private val errorDuplicateFileSequenceNoMsg = s"$fileSequenceNoLabel contains duplicates"
  private val errorFileSequenceNoLessThanOneMsg = s"$fileSequenceNoLabel must start from 1"

  private val errorMaxFileGroupSize = ResponseContents(BadRequestCode, errorMaxFileGroupSizeMsg)
  private val errorFileGroupSize = ResponseContents(BadRequestCode, errorFileGroupSizeMsg)
  private val errorMaxFileSequenceNo = ResponseContents(BadRequestCode, errorMaxFileSequenceNoMsg)
  private val errorDuplicateFileSequenceNo = ResponseContents(BadRequestCode, errorDuplicateFileSequenceNoMsg)
  private val errorFileSequenceNoLessThanOne = ResponseContents(BadRequestCode, errorFileSequenceNoLessThanOneMsg)

  override def refine[A](ar: AuthorisedRequest[A]): Future[Either[Result, ValidatedBatchFileUploadPayloadRequest[A]]] = {
    implicit val implicitAr: AuthorisedRequest[A] = ar
    ar.authorisedAs match {
      case BatchFileUploadCsp(_, _, _) | NonCsp(_, _) =>
        batchFileUploadPayloadValidationAction.refine(ar).map {
          case Right(validatedBatchFilePayloadRequest) =>

            implicit val implicitVpr: ValidatedPayloadRequest[A] = validatedBatchFilePayloadRequest
            val xml = validatedBatchFilePayloadRequest.xmlBody

            val declarationId = DeclarationId((xml \ declarationIdLabel).text)
            val fileGroupSize = FileGroupSize((xml \ fileGroupSizeLabel).text.trim.toInt)

            val files: Seq[BatchFileUploadFile] = (xml \ filesLabel \ "_").theSeq.collect {
              case file =>
                val fileSequenceNumber = FileSequenceNo((file \ fileSequenceNoLabel).text.trim.toInt)
                val documentType = DocumentType((file \ documentTypeLabel).text)
                BatchFileUploadFile(fileSequenceNumber, documentType)
              }

            val batchFileUpload = BatchFileUploadRequest(declarationId, fileGroupSize, files.sortWith(_.fileSequenceNo.value < _.fileSequenceNo.value))

            additionalValidation(batchFileUpload) match {
              case Right(_) =>
                Right(validatedBatchFilePayloadRequest.toValidatedBatchFileUploadPayloadRequest(batchFileUpload))
              case Left(errorResponse) =>
                Left(errorResponse.XmlResult)
            }
          case Left(result) => Left(result)
        }
      case _ => Future.successful(Left(ErrorResponse(FORBIDDEN, ForbiddenCode, "Not an authorized service").XmlResult.withConversationId))
    }
  }


  private def additionalValidation[A](batchFileUpload: BatchFileUploadRequest)(implicit vpr: ValidatedPayloadRequest[A]): Either[ErrorResponse, Unit] = {

    def maxFileGroupSize = validate(
      batchFileUpload,
      { b: BatchFileUploadRequest =>
        b.fileGroupSize.value <= declarationsConfigService.batchFileUploadConfig.fileGroupSizeMaximum},
      errorMaxFileGroupSize)

    def maxFileSequenceNo = validate(
      batchFileUpload,
      { b: BatchFileUploadRequest =>
        b.fileGroupSize.value >= b.files.last.fileSequenceNo.value },
      errorMaxFileSequenceNo)

    def fileGroupSize = validate(
      batchFileUpload,
      { b: BatchFileUploadRequest =>
        b.fileGroupSize.value == b.files.length },
      errorFileGroupSize)

    def duplicateFileSequenceNo = validate(
      batchFileUpload,
      { b: BatchFileUploadRequest =>
        b.files.distinct.length == b.files.length },
      errorDuplicateFileSequenceNo)

    def fileSequenceNoLessThanOne = validate(
      batchFileUpload,
      { b: BatchFileUploadRequest =>
        b.files.head.fileSequenceNo.value == 1},
      errorFileSequenceNoLessThanOne)

    maxFileGroupSize ++ maxFileSequenceNo ++ fileGroupSize ++ duplicateFileSequenceNo ++ fileSequenceNoLessThanOne match {
      case Seq() => Right(())
      case errors =>
        Left(new ErrorResponse(Status.BAD_REQUEST, BadRequestCode, "Payload did not pass validation", errors: _*))
    }
  }

  private def validate[A](batchFileUploadRequest: BatchFileUploadRequest,
                          rule: BatchFileUploadRequest => Boolean,
                          responseContents: ResponseContents)(implicit vpr: ValidatedPayloadRequest[A]): Seq[ResponseContents] = {

    def leftWithLogContainingValue(batchFileUploadRequest: BatchFileUploadRequest, responseContents: ResponseContents) = {
      logger.error(responseContents.message)
      Seq(responseContents)
    }

    if (rule(batchFileUploadRequest)) Seq() else leftWithLogContainingValue(batchFileUploadRequest, responseContents)
  }
}
