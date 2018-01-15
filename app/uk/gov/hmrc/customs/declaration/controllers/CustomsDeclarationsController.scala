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
import uk.gov.hmrc.customs.declaration.model.{Eori, Ids, RequestedVersion}
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
                                              customsDeclarationsBusinessService: CustomsDeclarationsBusinessService)
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

  private def validateHeaders(): ActionBuilder[Request] = {
    validateAccept(acceptHeaderValidation) andThen validateContentType(contentTypeValidation)
  }

  def submit(): Action[AnyContent] = validateHeaders().async(bodyParser = xmlOrEmptyBody) {
    implicit request =>

      logger.debug("submit", request.headers.headers)
      logger.debug("entered submit controller")
      lazy val maybeAcceptHeader = request.headers.get(ACCEPT)
      request.body.asXml match {
        case Some(xml) =>
          requestedVersionService.getVersionByAcceptHeader(maybeAcceptHeader).fold {
            logger.error(s"Requested version of Declarations API could not be resolved from Accept header: $maybeAcceptHeader")
            Future.successful(ErrorResponseInvalidVersionRequested.XmlResult)
          } {
            implicit version =>
              processXmlPayload(xml)
          }

        case _ =>
          logger.error(badlyFormedXmlMsg)
          Future.successful(ErrorResponse.errorBadRequest(badlyFormedXmlMsg).XmlResult)
      }
  }

  private def processXmlPayload(xml: NodeSeq)(implicit hc: HeaderCarrier, ver: RequestedVersion): Future[Result] = {
    (authoriseCspSubmission(xml) orElseIfInsufficientEnrolments authoriseNonCspSubmission(xml) orElse unauthorised)
      .map {
        case Right(ids) =>
          logger.info("exiting processXmlPayload", ids)
          NoContent.as(MimeTypes.XML).withHeaders("X-Conversation-ID" -> ids.conversationId.value)
        case Left(errorResponse) =>
          val msg = "Customs declaration submission failed."
          logger.debug(s"$msg error processing payload. HttpStatusCode=${errorResponse.httpStatusCode} Error=${errorResponse.message}")
          logger.error(msg)
          errorResponse.XmlResult
      }
      .recoverWith {
        case NonFatal(e) =>
          logger.error(s"Customs declaration submission failed. error=${e.getMessage}", e)
          Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
      }
  }

  private def authoriseCspSubmission(xml: NodeSeq)(implicit hc: HeaderCarrier, ver: RequestedVersion): Future[ProcessingResult] = {
    authorised(Enrolment(apiScopeKey) and AuthProviders(PrivilegedApplication)) {
      logger.info("Processing an authorised CSP submission.")
      customsDeclarationsBusinessService.authorisedCspSubmission(xml)
    }
  }

  private def authoriseNonCspSubmission(xml: NodeSeq)(implicit hc: HeaderCarrier, ver: RequestedVersion): Future[ProcessingResult] = {
    logger.info("Authorising a non-CSP request.")
    authorised(Enrolment(customsEnrolmentName) and AuthProviders(GovernmentGateway)).retrieve(Retrievals.authorisedEnrolments) {
      enrolments =>
        val maybeEori = findEoriInCustomsEnrolment(enrolments, hc.authorization)
        logger.debug(s"EORI from Customs enrolment for non-CSP request=$maybeEori")
        maybeEori match {
          case Some(_) =>
            logger.info("Processing an authorised non-CSP submission.")
            customsDeclarationsBusinessService.authorisedNonCspSubmission(xml)

          case _ => Future.successful(Left(ErrorResponseEoriNotFoundInCustomsEnrolment))
        }
    }
  }

  private def findEoriInCustomsEnrolment(enrolments: Enrolments, authHeader: Option[Authorization])(implicit hc: HeaderCarrier): Option[Eori] = {
    val maybeCustomsEnrolment = enrolments.getEnrolment(customsEnrolmentName)
    if (maybeCustomsEnrolment.isEmpty) {
      logger.warn(s"Customs enrolment $customsEnrolmentName not retrieved for authorised non-CSP call with Authorization header=${authHeader.map(_.value).getOrElse("")}")
    }
    for {
      customsEnrolment <- maybeCustomsEnrolment
      eoriIdentifier <- customsEnrolment.getIdentifier(customsEnrolmentEoriIdentifier)
    } yield Eori(eoriIdentifier.value)
  }

  private def unauthorised(authException: AuthorisationException)(implicit hc: HeaderCarrier): Future[Left[ErrorResponse, Ids]] = {
    val authorisationValue = hc.authorization.map(_.value).getOrElse("")
    logger.error(s"Unauthorized call with Authorization='$authorisationValue' . error=${authException.getMessage}", authException)
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
