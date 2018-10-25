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
import play.api.mvc.{ActionRefiner, RequestHeader, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left


/** Action builder that attempts to authorise request as a CSP or else NON CSP
  * <ul>
  * <li/>INPUT - `ValidatedHeadersRequest`
  * <li/>OUTPUT - `AuthorisedRequest` - authorised will be `AuthorisedAs.Csp` or `AuthorisedAs.NonCsp`
  * <li/>ERROR -
  * <ul>
  * <li/>401 if authorised as CSP but badge identifier not present for CSP
  * <li/>401 if authorised as NON CSP but enrolments does not contain an EORI.
  * <li/>401 if not authorised as CSP or NON CSP
  * <li/>500 on any downstream errors it returns 500
  * </ul>
  * </ul>
  */
@Singleton
class AuthAction @Inject()(
                            customsAuthService: CustomsAuthService,
                            headerValidator: HeaderValidator,
                            logger: DeclarationsLogger,
                            googleAnalyticsConnector: GoogleAnalyticsConnector,
                            declarationConfigService: DeclarationsConfigService
) extends ActionRefiner[ValidatedHeadersRequest, AuthorisedRequest] {

  override def refine[A](vhr: ValidatedHeadersRequest[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    implicit val implicitVhr: ValidatedHeadersRequest[A] = vhr
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    val isNrs = declarationConfigService.nrsConfig.nrsEnabled

    authAsCspWithMandatoryAuthHeaders(isNrs).flatMap{
      case Right(maybeAuthorisedAsCspWithBadgeIdentifierAndNrsData) =>
        maybeAuthorisedAsCspWithBadgeIdentifierAndNrsData.fold{
          customsAuthService.authAsNonCsp(isNrs).map[Either[Result, AuthorisedRequest[A]]]{
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

  private def authAsCspWithMandatoryAuthHeaders[A](isNrs: Boolean)(implicit vhr: HasRequest[A] with HasConversationId with HasAnalyticsValues, hc: HeaderCarrier): Future[Either[ErrorResponse, Option[AuthorisedAsCsp]]] = {

    val eventualAuthWithBadgeId: Future[Either[ErrorResponse, Option[AuthorisedAsCsp]]] = customsAuthService.authAsCsp(isNrs).map{
      case Right((isCsp, maybeNrsRetrievalData)) =>
        if (isCsp) {
          eitherCspAuthData(maybeNrsRetrievalData).right.map(authAsCsp => Some(authAsCsp))
        } else {
          Right(None)
        }
      case Left(errorResponse) =>
        Left(errorResponse)
    }

    eventualAuthWithBadgeId
  }

  protected def eitherCspAuthData[A](maybeNrsRetrievalData: Option[NrsRetrievalData])(implicit vhr: HasRequest[A] with HasConversationId with HasAnalyticsValues): Either[ErrorResponse, AuthorisedAsCsp] = {

    eitherBadgeIdentifier.right.map(badgeId => Csp(badgeId, maybeNrsRetrievalData))
  }

  protected def eitherBadgeIdentifier[A](implicit vhr: HasRequest[A] with HasConversationId with HasAnalyticsValues): Either[ErrorResponse, BadgeIdentifier] = {
    headerValidator.eitherBadgeIdentifier.left.map{errorResponse =>
      googleAnalyticsConnector.failure(errorResponse.message)
      errorResponse
    }
  }

}
