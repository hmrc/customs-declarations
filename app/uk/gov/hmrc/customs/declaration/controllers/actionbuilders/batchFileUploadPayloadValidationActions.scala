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
import play.api.mvc.{ActionRefiner, Result}
import play.mvc.Http.Status.FORBIDDEN
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ForbiddenCode
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, BatchFileUploadProperties, ValidatedBatchFileUploadPayloadRequest}
import uk.gov.hmrc.customs.declaration.services.BatchFileUploadXmlValidationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BatchFileUploadPayloadValidationAction @Inject()(batchFileUploadXmlValidationService: BatchFileUploadXmlValidationService,
                                                       logger: DeclarationsLogger,
                                                       googleAnalyticsConnector: GoogleAnalyticsConnector)
    extends PayloadValidationAction(batchFileUploadXmlValidationService, logger, Some(googleAnalyticsConnector))

class BatchFileUploadPayloadValidationComposedAction @Inject()(val batchFileUploadPayloadValidationAction: BatchFileUploadPayloadValidationAction,
                                                               val logger: DeclarationsLogger)
  extends ActionRefiner[AuthorisedRequest, ValidatedBatchFileUploadPayloadRequest]  {

  private val declarationIdLabel = "DeclarationID"
  private val documentationTypeLabel = "DocumentType"
  private val groupSizeLabel = "FileGroupSize"
  private val sequenceNumber = "FileSequenceNo"

  override def refine[A](ar: AuthorisedRequest[A]): Future[Either[Result, ValidatedBatchFileUploadPayloadRequest[A]]] = {
    implicit val implicitAr: AuthorisedRequest[A] = ar
    ar.authorisedAs match {
      case NonCsp(_, _) =>
        batchFileUploadPayloadValidationAction.refine(ar).map {
          case Right(validatedBatchFilePayloadRequest) =>
            //TODO extract & validate values
            Right(validatedBatchFilePayloadRequest.toValidatedBatchFileUploadPayloadRequest(
              DeclarationId("decId"), FileGroupSize(2),
              List(BatchFileUploadProperties(SequenceNumber(1), DocumentationType("doctype1")), BatchFileUploadProperties(SequenceNumber(2), DocumentationType("doctype2")))
            ))
          case Left(b) => Left(b)
        }
      case _ => Future.successful(Left(ErrorResponse(FORBIDDEN, ForbiddenCode, "Not an authorized service").XmlResult.withConversationId))
    }
  }

}
