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

package uk.gov.hmrc.customs.declaration.controllers.actionbuilders.upscan

import javax.inject.{Inject, Singleton}
import play.api.http.Status
import play.api.mvc.{ActionRefiner, Result}
import play.mvc.Http.Status.FORBIDDEN
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, HttpStatusCodeShortDescriptions, ResponseContents}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.PayloadValidationAction
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.model.upscan.DocumentType
import uk.gov.hmrc.customs.declaration.model.{FileGroupSize, _}
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, FileUploadXmlValidationService}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Node

@Singleton
class FileUploadPayloadValidationAction @Inject()(fileUploadXmlValidationService: FileUploadXmlValidationService,
                                                  logger: DeclarationsLogger)
                                                 (implicit ec: ExecutionContext)
    extends PayloadValidationAction(fileUploadXmlValidationService, logger)

class FileUploadPayloadValidationComposedAction @Inject()(val fileUploadPayloadValidationAction: FileUploadPayloadValidationAction,
                                                          val logger: DeclarationsLogger,
                                                          val declarationsConfigService: DeclarationsConfigService)
                                                         (implicit ec: ExecutionContext)
  extends ActionRefiner[AuthorisedRequest, ValidatedFileUploadPayloadRequest] with HttpStatusCodeShortDescriptions {

  private val declarationIdLabel = "DeclarationID"
  private val documentTypeLabel = "DocumentType"
  private val fileGroupSizeLabel = "FileGroupSize"
  private val fileSequenceNoLabel = "FileSequenceNo"
  private val filesLabel = "Files"
  private val fileLabel = "File"
  private val successRedirectLabel = "SuccessRedirect"
  private val errorRedirectLabel = "ErrorRedirect"

  private val errorMaxFileGroupSizeMsg = s"$fileGroupSizeLabel exceeds ${declarationsConfigService.fileUploadConfig.fileGroupSizeMaximum} limit"
  private val errorFileGroupSizeMsg = s"$fileGroupSizeLabel does not match number of $fileLabel elements"
  private val errorMaxFileSequenceNoMsg = s"$fileSequenceNoLabel must not be greater than $fileGroupSizeLabel"
  private val errorDuplicateFileSequenceNoMsg = s"$fileSequenceNoLabel contains duplicates"
  private val errorFileSequenceNoLessThanOneMsg = s"$fileSequenceNoLabel must start from 1"

  private val errorMaxFileGroupSize = ResponseContents(BadRequestCode, errorMaxFileGroupSizeMsg)
  private val errorFileGroupSize = ResponseContents(BadRequestCode, errorFileGroupSizeMsg)
  private val errorMaxFileSequenceNo = ResponseContents(BadRequestCode, errorMaxFileSequenceNoMsg)
  private val errorDuplicateFileSequenceNo = ResponseContents(BadRequestCode, errorDuplicateFileSequenceNoMsg)
  private val errorFileSequenceNoLessThanOne = ResponseContents(BadRequestCode, errorFileSequenceNoLessThanOneMsg)

  override def refine[A](ar: AuthorisedRequest[A]): Future[Either[Result, ValidatedFileUploadPayloadRequest[A]]] = {
    implicit val implicitAr: AuthorisedRequest[A] = ar
    ar.authorisedAs match {
      case CspWithEori(_, _, _) | NonCsp(_, _) =>
        fileUploadPayloadValidationAction.refine(ar).map {
          case Right(validatedFilePayloadRequest) =>

            implicit val implicitVpr: ValidatedPayloadRequest[A] = validatedFilePayloadRequest
            val xml = validatedFilePayloadRequest.xmlBody

            val declarationId = DeclarationId((xml \ declarationIdLabel).text)
            val fileGroupSize = FileGroupSize((xml \ fileGroupSizeLabel).text.trim.toInt)

            val files: Seq[FileUploadFile] = (xml \ filesLabel \ "_").theSeq.collect {
              case file =>
                val fileSequenceNumber = FileSequenceNo((file \ fileSequenceNoLabel).text.trim.toInt)
                val maybeDocumentTypeText = maybeElement(file, documentTypeLabel)
                val documentType = if (maybeElement(file, documentTypeLabel).isEmpty) None else Some(DocumentType(maybeDocumentTypeText.get))
                FileUploadFile(fileSequenceNumber, documentType, maybeElement(file, successRedirectLabel), maybeElement(file, errorRedirectLabel))

            }

            val fileUpload = FileUploadRequest(declarationId, fileGroupSize, files.sortWith(_.fileSequenceNo.value < _.fileSequenceNo.value))

            additionalValidation(fileUpload) match {
              case Right(_) =>
                Right(validatedFilePayloadRequest.toValidatedFileUploadPayloadRequest(fileUpload))
              case Left(errorResponse) =>
                Left(errorResponse.XmlResult)
            }
          case Left(result) => Left(result)
        }
      case _ => Future.successful(Left(ErrorResponse(FORBIDDEN, ForbiddenCode, "Not an authorized service").XmlResult.withConversationId))
    }
  }

  private def maybeElement(file: Node, label: String): Option[String] = {
    val elementText = (file \ label).text
    if (elementText.trim.isEmpty) None else Some(elementText)
  }


  private def additionalValidation[A](fileUpload: FileUploadRequest)(implicit vpr: ValidatedPayloadRequest[A]): Either[ErrorResponse, Unit] = {

    def maxFileGroupSize = validate(
      fileUpload,
      { b: FileUploadRequest =>
        b.fileGroupSize.value <= declarationsConfigService.fileUploadConfig.fileGroupSizeMaximum},
      errorMaxFileGroupSize)

    def maxFileSequenceNo = validate(
      fileUpload,
      { b: FileUploadRequest =>
        b.fileGroupSize.value >= b.files.last.fileSequenceNo.value },
      errorMaxFileSequenceNo)

    def fileGroupSize = validate(
      fileUpload,
      { b: FileUploadRequest =>
        b.fileGroupSize.value == b.files.length },
      errorFileGroupSize)

    def duplicateFileSequenceNo = validate(
      fileUpload,
      { b: FileUploadRequest =>
        b.files.distinct.length == b.files.length },
      errorDuplicateFileSequenceNo)

    def fileSequenceNoLessThanOne = validate(
      fileUpload,
      { b: FileUploadRequest =>
        b.files.head.fileSequenceNo.value == 1},
      errorFileSequenceNoLessThanOne)

    maxFileGroupSize ++ maxFileSequenceNo ++ fileGroupSize ++ duplicateFileSequenceNo ++ fileSequenceNoLessThanOne match {
      case Seq() => Right(())
      case errors =>
        Left(new ErrorResponse(Status.BAD_REQUEST, BadRequestCode, "Payload did not pass validation", errors: _*))
    }
  }

  private def validate[A](fileUploadRequest: FileUploadRequest,
                          rule: FileUploadRequest => Boolean,
                          responseContents: ResponseContents)(implicit vpr: ValidatedPayloadRequest[A]): Seq[ResponseContents] = {

    def leftWithLogContainingValue(fileUploadRequest: FileUploadRequest, responseContents: ResponseContents) = {
      logger.error(responseContents.message)
      Seq(responseContents)
    }

    if (rule(fileUploadRequest)) Seq() else leftWithLogContainingValue(fileUploadRequest, responseContents)
  }
}
