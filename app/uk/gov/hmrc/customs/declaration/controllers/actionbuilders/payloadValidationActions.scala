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
import play.api.mvc.{ActionRefiner, AnyContent, Result}
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services._

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.{NodeSeq, SAXException}

@Singleton
class SubmitPayloadValidationAction @Inject() (xmlValidationService: SubmissionXmlValidationService, logger: DeclarationsLogger, googleAnalyticsConnector: GoogleAnalyticsConnector) extends PayloadValidationAction(xmlValidationService, logger, Some(googleAnalyticsConnector))

@Singleton
class ClearancePayloadValidationAction @Inject() (xmlValidationService: ClearanceXmlValidationService, logger: DeclarationsLogger, googleAnalyticsConnector: GoogleAnalyticsConnector) extends PayloadValidationAction(xmlValidationService, logger, Some(googleAnalyticsConnector))

@Singleton
class AmendPayloadValidationAction @Inject() (xmlValidationService: AmendXmlValidationService, logger: DeclarationsLogger) extends PayloadValidationAction(xmlValidationService, logger, None)

@Singleton
class CancelPayloadValidationAction @Inject() (xmlValidationService: CancellationXmlValidationService, logger: DeclarationsLogger, googleAnalyticsConnector: GoogleAnalyticsConnector) extends PayloadValidationAction(xmlValidationService, logger, Some(googleAnalyticsConnector))

abstract class PayloadValidationAction(val xmlValidationService: XmlValidationService, logger: DeclarationsLogger, maybeGoogleAnalyticsConnector: Option[GoogleAnalyticsConnector]) extends ActionRefiner[AuthorisedRequest, ValidatedPayloadRequest] {

  override def refine[A](ar: AuthorisedRequest[A]): Future[Either[Result, ValidatedPayloadRequest[A]]] = {
    implicit val implicitAr = ar

    validateXml
  }

  private def validateXml[A](implicit ar: AuthorisedRequest[A]): Future[Either[Result, ValidatedPayloadRequest[A]]] = {
    lazy val errorMessage = "Request body does not contain a well-formed XML document."
    lazy val errorNotWellFormed = ErrorResponse.errorBadRequest(errorMessage).XmlResult.withConversationId

    def validate(xml: NodeSeq): Future[Either[Result, ValidatedPayloadRequest[A]] with Product with Serializable] =
      xmlValidationService.validate(xml).map{ _ =>
        logger.debug("XML payload validated.")
        Right(ar.toValidatedPayloadRequest(xml))
      }
      .recover {
        case saxe: SAXException =>
          val msg = "Payload did not pass validation against the schema."
          logger.debug(msg, saxe)
          logger.error(msg)
          maybeGoogleAnalyticsConnector.map(conn => conn.failure(msg))
          Left(ErrorResponse
            .errorBadRequest("Payload is not valid according to schema")
            .withErrors(xmlValidationErrors(saxe): _*).XmlResult.withConversationId)
        case NonFatal(e) =>
          val msg = "Error validating payload."
          logger.debug(msg, e)
          logger.error(msg)
          Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
      }

    ar.asInstanceOf[AuthorisedRequest[AnyContent]].body.asXml.fold[Future[Either[Result, ValidatedPayloadRequest[A]]]]{
      maybeGoogleAnalyticsConnector.map(conn => conn.failure(errorMessage))
      Future.successful(Left(errorNotWellFormed))
    }{
      xml => validate(xml)
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
}
