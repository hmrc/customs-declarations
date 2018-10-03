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
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedUploadPayloadRequest}
import uk.gov.hmrc.customs.declaration.services.FileUploadXmlValidationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class FileUploadPayloadValidationAction @Inject() (fileUploadXmlValidationService: FileUploadXmlValidationService,
                                                   logger: DeclarationsLogger, googleAnalyticsConnector: GoogleAnalyticsConnector)
  extends PayloadValidationAction(fileUploadXmlValidationService, logger, Some(googleAnalyticsConnector))

@Singleton
class FileUploadPayloadValidationComposedAction @Inject()(val fileUploadPayloadValidationAction: FileUploadPayloadValidationAction,
                                                          val logger: DeclarationsLogger)
  extends ActionRefiner[AuthorisedRequest, ValidatedUploadPayloadRequest] {

  private val declarationIdPropertyName = "declarationID"
  private val documentationTypePropertyName = "documentationType"

  override def refine[A](ar: AuthorisedRequest[A]): Future[Either[Result, ValidatedUploadPayloadRequest[A]]] = {
    implicit val implicitAr: AuthorisedRequest[A] = ar
    ar.authorisedAs match {
      case NonCsp(_, _) =>
        fileUploadPayloadValidationAction.refine(ar).map {
          case Right(validatedPayloadRequest) =>
            Right(validatedPayloadRequest.toValidatedUploadPayloadRequest(
              DeclarationId((validatedPayloadRequest.xmlBody \ declarationIdPropertyName).text),
              DocumentationType((validatedPayloadRequest.xmlBody \ documentationTypePropertyName).text)))
          case Left(b) => Left(b)
        }
      case _ => Future.successful(Left(ErrorResponse(FORBIDDEN, ForbiddenCode, "Not an authorized service").XmlResult.withConversationId))
    }
  }
}
