/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.{ActionRefiner, AnyContent, Result}
import uk.gov.hmrc.customs.declaration.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, SAXException}

@Singleton
class SubmitPayloadValidationAction @Inject() (xmlValidationService: SubmissionXmlValidationService,
                                               logger: DeclarationsLogger)
                                              (implicit ec: ExecutionContext)
  extends PayloadValidationAction(xmlValidationService, logger) {
  override def executionContext: ExecutionContext = ec
}

@Singleton
class AmendPayloadValidationAction @Inject() (xmlValidationService: AmendXmlValidationService,
                                              logger: DeclarationsLogger)
                                             (implicit ec: ExecutionContext)
  extends PayloadValidationAction(xmlValidationService, logger) {
  override def executionContext: ExecutionContext = ec
}

@Singleton
class ArrivalNotificationPayloadValidationAction @Inject() (xmlValidationService: ArrivalNotificationXmlValidationService,
                                                            logger: DeclarationsLogger)
                                                           (implicit ec: ExecutionContext)
  extends PayloadValidationAction(xmlValidationService, logger) {
  override def executionContext: ExecutionContext = ec
}

@Singleton
class CancelPayloadValidationAction @Inject() (xmlValidationService: CancellationXmlValidationService,
                                               logger: DeclarationsLogger)
                                              (implicit ec: ExecutionContext)
  extends PayloadValidationAction(xmlValidationService, logger) {
  override def executionContext: ExecutionContext = ec
}

abstract class PayloadValidationAction(val xmlValidationService: XmlValidationService,
                                       logger: DeclarationsLogger)
                                      (implicit ec: ExecutionContext)
  extends ActionRefiner[AuthorisedRequest, ValidatedPayloadRequest] {

  override def refine[A](ar: AuthorisedRequest[A]): Future[Either[Result, ValidatedPayloadRequest[A]]] = {
    implicit val implicitAr = ar
    lazy val errorMessage = "Request body does not contain a well-formed XML document."

    ar.body match {
      case content: AnyContent =>
        content.asXml
          .map(validateXml(_))
          .getOrElse(Future.successful(Left(errorResponse(errorMessage, content.toString))))

      case unexpectedBody => Future.successful(Left(errorResponse(errorMessage, unexpectedBody.toString)))
    }
  }

  private def validateXml[A](xml: NodeSeq)(implicit ar: AuthorisedRequest[A]): Future[Either[Result, ValidatedPayloadRequest[A]]] = {
    xmlValidationService.validate(xml)
      .map { _ =>
        logger.debug("XML payload validated")
        Right(ar.toValidatedPayloadRequest(xml))
      }
      .recover {
        case saxe: SAXException =>
          val msg = "Payload is not valid according to schema"
          logger.debug(s"$msg:\n${xml.toString()}", saxe)
          Left(errorResponse(msg, xml.toString(), xmlValidationErrors(saxe)*))
        case NonFatal(e) =>
          val msg = "Error validating payload."
          logger.debug(s"$msg:\n${xml.toString()}", e)
          logger.warn(msg)
          Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
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

  private def errorResponse[A](msg: String, requestBodyAsString: String, contents: ResponseContents*)(implicit ar: AuthorisedRequest[A]): Result = {
    logger.debug(s"$msg:\n$requestBodyAsString")
    logger.warn(msg)
    ErrorResponse.errorBadRequest(msg)
      .withErrors(contents*)
      .XmlResult
      .withConversationId
  }
}
