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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{Ids, RequestType}
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.{NodeSeq, SAXException}

@Singleton
class CustomsDeclarationsBusinessService @Inject()(logger: DeclarationsLogger,
                                                   communication: CommunicationService,
                                                   xmlValidationServiceProvider: XmlValidationServiceProvider) {

  type ValidationResult = Either[ErrorResponse, Unit]

  def authorisedCspSubmission(xml: NodeSeq)(implicit hc: HeaderCarrier, ids: Ids): Future[ProcessingResult] = {
    sendIfValid(xml)
  }

  def authorisedNonCspSubmission(xml: NodeSeq)(implicit hc: HeaderCarrier, ids: Ids): Future[ProcessingResult] = {
    sendIfValid(xml)
  }

  private def sendIfValid(xml: NodeSeq)(implicit hc: HeaderCarrier, ids: Ids): Future[ProcessingResult] = {
    validateXml(xml) thenProcessWith prepareAndSend(xml)
  }

  private def validateXml(xml: NodeSeq)(implicit hc: HeaderCarrier, ids: Ids) = {
    val service: XmlValidationService = ids.requestType match {
      case RequestType.Submit => xmlValidationServiceProvider.submission
      case RequestType.Cancel => xmlValidationServiceProvider.cancellation
    }
    service.validate(xml).map(Right(_))
      .recover {
        case saxe: SAXException =>
          val msg = "Payload is not valid according to schema"
          logger.error(msg, ids)
          Left(ErrorResponse
            .errorBadRequest(msg)
            .withErrors(xmlValidationErrors(saxe): _*))
      }
  }

  private def xmlValidationErrors(saxe: SAXException): Seq[ResponseContents] = {
    @annotation.tailrec
    def loop(thr: Exception, acc: List[ResponseContents]): List[ResponseContents] = {
      val newAcc = ResponseContents("xml_validation_error", thr.getMessage) :: acc
      thr match {
        case saxError: SAXException if Option(saxError.getException).isDefined => loop(saxError.getException, newAcc)
        case _ => newAcc
      }
    }

    loop(saxe, Nil)
  }

  private def prepareAndSend(xml: NodeSeq)(implicit hc: HeaderCarrier, ids: Ids): Future[ProcessingResult] = {
    communication.prepareAndSend(xml).map(Right(_))
  }

  private implicit class ValidationFutureOps(validationFuture: Future[ValidationResult]) {
    def thenProcessWith(processingFuture: => Future[ProcessingResult]): Future[ProcessingResult] = {
      validationFuture.flatMap {
        case Right(_) => processingFuture
        case Left(errorResponse) => Future.successful(Left(errorResponse))
      }
    }
  }
}
