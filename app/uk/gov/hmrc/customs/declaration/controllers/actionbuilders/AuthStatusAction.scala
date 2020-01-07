/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, UnauthorizedCode}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.Csp
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedHeadersStatusRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left
import scala.util.control.NonFatal


@Singleton
class AuthStatusAction @Inject()(override val authConnector: AuthConnector,
                                 logger: DeclarationsLogger)
                                (implicit ec: ExecutionContext)
  extends ActionRefiner[ValidatedHeadersStatusRequest, AuthorisedRequest] with AuthorisedFunctions  {

  protected def executionContext: ExecutionContext = ec
  private val errorResponseUnauthorisedGeneral = ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")
  private val customsDeclarationEnrolment = "write:customs-declaration"

  override def refine[A](vhsr: ValidatedHeadersStatusRequest[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    implicit val implicitVhsr: ValidatedHeadersStatusRequest[A] = vhsr
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    authorised(Enrolment(customsDeclarationEnrolment) and AuthProviders(PrivilegedApplication)) {
      logger.debug(s"Successfully authorised status CSP PrivilegedApplication with $customsDeclarationEnrolment enrolment")
      Future.successful(Right(vhsr.toAuthorisedRequest(Csp(None, Some(vhsr.badgeIdentifier), None)))) // Simply won't get through if no MRN is specified
    }.recover{
      case NonFatal(ae: AuthorisationException) =>
        logger.debug(s"No authorisation for status CSP PrivilegedApplication with $customsDeclarationEnrolment enrolment", ae)
        Left(errorResponseUnauthorisedGeneral.XmlResult.withConversationId)
      case NonFatal(e) =>
        logger.error(s"Error when authorising for status CSP PrivilegedApplication with $customsDeclarationEnrolment enrolment", e)
        Left(ErrorInternalServerError.XmlResult.withConversationId)
    }
  }
}
