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
import play.api.mvc.{ActionRefiner, RequestHeader, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Left


/** Action builder that attempts to authorise request as a CSP or else NON CSP
  * <ul>
  * <li/>INPUT - `ValidatedHeadersRequest`
  * <li/>OUTPUT - `AuthorisedRequest` - authorised will be `AuthorisedAs.Csp` or `AuthorisedAs.NonCsp`
  * <li/>ERROR -
  * <ul>
  * <li/>400 if authorised as CSP but badge identifier not present for CSP
  * <li/>401 if authorised as non-CSP but enrolments does not contain an EORI.
  * <li/>401 if not authorised as CSP or non-CSP
  * <li/>500 on any downstream errors returning 500
  * </ul>
  * </ul>
  */
@Singleton
class AuthAction @Inject()(customsAuthService: CustomsAuthService,
                           headerValidator: HeaderWithContentTypeValidator,
                           logger: DeclarationsLogger,
                           declarationConfigService: DeclarationsConfigService)
                          (implicit ec: ExecutionContext)
  extends ActionRefiner[ValidatedHeadersRequest, AuthorisedRequest] {

  protected[this] def requestRetrievalsForEndpoint: Boolean = true
  protected[this] def executionContext: ExecutionContext = ec

  override def refine[A](vhr: ValidatedHeadersRequest[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    implicit val implicitVhr: ValidatedHeadersRequest[A] = vhr
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    val requestRetrievals = requestRetrievalsForEndpoint && declarationConfigService.nrsConfig.nrsEnabled
    logger.debug(s"retrievals being requested - $requestRetrievals")

    authAsCspWithMandatoryAuthHeaders(requestRetrievals).flatMap{
      case Right(maybeAuthorisedAsCspWithIdentifierHeadersAndNrsData) =>
        maybeAuthorisedAsCspWithIdentifierHeadersAndNrsData.fold{
          customsAuthService.authAsNonCsp(requestRetrievals).map[Either[Result, AuthorisedRequest[A]]]{
            case Left(errorResponse) =>
              Left(errorResponse.XmlResult.withConversationId)
            case Right(nonCspData) =>
              Right(vhr.toNonCspAuthorisedRequest(nonCspData.eori, nonCspData.retrievalData))
          }
        }{ cspData =>
          Future.successful(Right(vhr.toCspAuthorisedRequest(cspData)))
        }
      case Left(result) =>
        Future.successful(Left(result.XmlResult.withConversationId))
    }
  }

  private def authAsCspWithMandatoryAuthHeaders[A](requestRetrievals: Boolean)
                                                  (implicit vhr: HasRequest[A] with HasConversationId, hc: HeaderCarrier): Future[Either[ErrorResponse, Option[AuthorisedAsCsp]]] = {

    val eventualAuthWithIdentifierHeaders: Future[Either[ErrorResponse, Option[AuthorisedAsCsp]]] =
      customsAuthService.authAsCsp(requestRetrievals).map {
        case Right((isCsp, maybeNrsRetrievalData)) =>
          if (isCsp) {
            eitherCspAuthData(maybeNrsRetrievalData).right.map(authAsCsp => Some(authAsCsp))
          } else {
            Right(None)
          }
        case Left(errorResponse) =>
          Left(errorResponse)
    }
    eventualAuthWithIdentifierHeaders
  }

  protected def eitherCspAuthData[A](maybeNrsRetrievalData: Option[NrsRetrievalData])(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, AuthorisedAsCsp] = {
    eitherBadgeIdentifier(allowNone = false).right.map(badgeId => Csp(None, badgeId, maybeNrsRetrievalData))
  }

  protected def eitherBadgeIdentifier[A](allowNone: Boolean)(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[BadgeIdentifier]] = {
    headerValidator.eitherBadgeIdentifier(allowNone = allowNone)
  }

}
