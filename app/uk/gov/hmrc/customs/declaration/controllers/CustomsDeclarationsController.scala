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

package uk.gov.hmrc.customs.declaration.controllers

import javax.inject.{Inject, Singleton}

import play.api.http.MimeTypes
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.UnauthorizedCode
import uk.gov.hmrc.customs.declaration.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{ConversationId, Eori, Ids}
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.NodeSeq

@Singleton
class CustomsDeclarationsController @Inject()(logger: DeclarationsLogger,
                                              customsConfigService: CustomsConfigService,
                                              override val authConnector: MicroserviceAuthConnector,
                                              override val requestedVersionService: RequestedVersionService,
                                              customsDeclarationsBusinessService: CustomsDeclarationsBusinessService,
                                              uuidService: UuidService
                                             )
  extends BaseController with HeaderValidator with AuthorisedFunctions {

  override val declarationsLogger: DeclarationsLogger = logger

  private lazy val apiScopeKey = customsConfigService.apiDefinitionConfig.apiScope
  private val badlyFormedXmlMsg = "Request body does not contain a well-formed XML document."

  private lazy val customsEnrolmentName = customsConfigService.customsEnrolmentConfig.customsEnrolmentName
  private lazy val customsEnrolmentEoriIdentifier = customsConfigService.customsEnrolmentConfig.eoriIdentifierName

  private lazy val ErrorResponseInvalidVersionRequested =
    ErrorResponse(NOT_ACCEPTABLE, "INVALID_VERSION_REQUESTED", "Invalid API version requested")

  private lazy val ErrorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")

  private lazy val ErrorResponseUnauthorisedGeneral =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) => Right(AnyContentAsXml(xml))
    case _ => Right(AnyContentAsEmpty)
  })

  private def validateHeaders[A](implicit request: Request[A]): Option[Seq[ErrorResponse]] = {
    val seq = Seq(validateAccept, validateContentType).filter(_.nonEmpty).map(_.get)
    if(seq.isEmpty) None else Some(seq)
  }

  private def processRequest(ids: Ids)(implicit request: Request[AnyContent]): Future[Result] = {
    lazy val maybeAcceptHeader = request.headers.get(ACCEPT)
    request.body.asXml match {
      case Some(xml) =>
        requestedVersionService.getVersionByAcceptHeader(maybeAcceptHeader).fold {
          logger.error("Requested version is not valid. Processing failed.", ids)
          Future.successful(ErrorResponseInvalidVersionRequested.XmlResult.withHeaders(conversationIdHeader(ids)))
        } {
          version =>
            implicit val extendedIds = ids.copy(maybeRequestedVersion = Some(version))
            processXmlPayload(xml)
        }

      case _ =>
        logger.error(badlyFormedXmlMsg, ids)
        Future.successful(ErrorResponse.errorBadRequest(badlyFormedXmlMsg).XmlResult.withHeaders(conversationIdHeader(ids)))
    }
  }

  def submit(): Action[AnyContent] = Action.async(bodyParser = xmlOrEmptyBody) {
    implicit request =>

      val conversationId = uuidService.uuid().toString
      val basicIds = Ids(ConversationId(conversationId))
      logger.debug(s"Request received. Payload = ${request.body.toString} headers = ${request.headers.headers}", basicIds)

      validateHeaders(request) match {
        case Some(seq) =>
          val errors = seq.mkString(" ")
          logger.error(s"Header validation failed due to $errors", basicIds)
          Future.successful(seq.head.XmlResult)
        case _ => processRequest(basicIds)
      }

  }

  private def conversationIdHeader(wrapper: Ids) = {
    "X-Conversation-ID" -> wrapper.conversationId.value
  }

  private def processXmlPayload(xml: NodeSeq)(implicit hc: HeaderCarrier, ids: Ids): Future[Result] = {
    (authoriseCspSubmission(xml) orElseIfInsufficientEnrolments authoriseNonCspSubmission(xml) orElse unauthorised)
      .map {
        case Right(identifiers) =>
          logger.info("Request processed successfully", identifiers)
          NoContent.as(MimeTypes.XML).withHeaders(conversationIdHeader(identifiers))
        case Left(errorResponse) =>
          errorResponse.XmlResult.withHeaders(conversationIdHeader(ids))
      }
      .recoverWith {
        case NonFatal(_) =>
          logger.error("Processing error.", ids)
          Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult.withHeaders(conversationIdHeader(ids)))
      }
  }

  private def authoriseCspSubmission(xml: NodeSeq)(implicit hc: HeaderCarrier, ids: Ids): Future[ProcessingResult] = {
    authorised(Enrolment(apiScopeKey) and AuthProviders(PrivilegedApplication)) {
      customsDeclarationsBusinessService.authorisedCspSubmission(xml)
    }
  }

  private def authoriseNonCspSubmission(xml: NodeSeq)(implicit hc: HeaderCarrier, ids: Ids): Future[ProcessingResult] = {
    authorised(Enrolment(customsEnrolmentName) and AuthProviders(GovernmentGateway)).retrieve(Retrievals.authorisedEnrolments) {
      enrolments =>
        val maybeEori = findEoriInCustomsEnrolment(enrolments, hc.authorization)
        maybeEori match {
          case Some(_) =>
            customsDeclarationsBusinessService.authorisedNonCspSubmission(xml)

          case _ => Future.successful(Left(ErrorResponseEoriNotFoundInCustomsEnrolment))
        }
    }
  }

  private def findEoriInCustomsEnrolment(enrolments: Enrolments, authHeader: Option[Authorization])(implicit hc: HeaderCarrier, ids: Ids): Option[Eori] = {
    val maybeCustomsEnrolment = enrolments.getEnrolment(customsEnrolmentName)
    if (maybeCustomsEnrolment.isEmpty) {
      logger.debug(s"Customs enrolment $customsEnrolmentName not retrieved for authorised non-CSP call with Authorization header=${authHeader.map(_.value).getOrElse("")}", ids)
    }
    for {
      customsEnrolment <- maybeCustomsEnrolment
      eoriIdentifier <- customsEnrolment.getIdentifier(customsEnrolmentEoriIdentifier)
    } yield Eori(eoriIdentifier.value)
  }

  private def unauthorised(authException: AuthorisationException)(implicit hc: HeaderCarrier, ids: Ids): Future[Left[ErrorResponse, Ids]] = {
    logger.error("User is not authorised", ids)
    Future.successful(Left(ErrorResponseUnauthorisedGeneral))
  }

  private implicit class AuthOps(val authFuture: Future[ProcessingResult]) {
    def orElseIfInsufficientEnrolments(elseFuture: => Future[ProcessingResult]): Future[ProcessingResult] = {
      authFuture recoverWith {
        case _: InsufficientEnrolments => elseFuture
      }
    }

    def orElse(elseFuture: AuthorisationException => Future[ProcessingResult]): Future[ProcessingResult] =
      authFuture recoverWith {
        case authException: AuthorisationException => elseFuture(authException)
      }
  }

}
