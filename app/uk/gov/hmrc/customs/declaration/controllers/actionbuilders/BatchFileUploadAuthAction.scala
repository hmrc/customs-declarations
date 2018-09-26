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
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, AuthorisationException, Enrolment}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorInternalServerError, errorBadRequest}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left
import scala.util.control.NonFatal

@Singleton
class BatchFileUploadAuthAction @Inject()(override val authConnector: AuthConnector,
                                          logger: DeclarationsLogger,
                                          googleAnalyticsConnector: GoogleAnalyticsConnector,
                                          declarationConfigService: DeclarationsConfigService)
  extends AuthAction(authConnector, logger, googleAnalyticsConnector, declarationConfigService) {

  private lazy val xEoriIdentifierRegex = "^[0-9A-Za-z]{1,17}$".r
  private val errorResponseEoriIdentifierHeaderMissing = errorBadRequest(s"$XEoriIdentifierHeaderName header is missing or invalid")

  override def refine[A](vhr: ValidatedHeadersRequest[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    implicit val implicitVhr: ValidatedHeadersRequest[A] = vhr

    def futureAuthoriseAsCsp = if (declarationConfigService.nrsConfig.nrsEnabled) futureAuthoriseAsBatchFileUploadCspNrsEnabled else futureAuthoriseAsBatchFileUploadCspNrsDisabled
    def authoriseAsNonCsp = if (declarationConfigService.nrsConfig.nrsEnabled) authoriseAsNonCspNrsEnabled else authoriseAsNonCspNrsDisabled

    futureAuthoriseAsCsp.flatMap{
      case Right(maybeAuthorisedAsCspWithBadgeIdAndEori) =>
        maybeAuthorisedAsCspWithBadgeIdAndEori.fold {
          authoriseAsNonCsp.map[Either[Result, AuthorisedRequest[A]]] {
            case Left(errorResponse) =>
              Left(errorResponse.XmlResult.withConversationId)
            case Right(a) => Right(a)
          }
        } {pair =>
          Future.successful(Right(vhr.toBatchFileUploadCspAuthorisedRequest(pair._1.asInstanceOf[BadgeIdentifierEoriPair].badgeIdentifier,
            pair._1.asInstanceOf[BadgeIdentifierEoriPair].eori, pair._2.asInstanceOf[Option[CspRetrievalData]])))
        }
      case Left(result) =>
        Future.successful(Left(result))
    }
  }

  protected def futureAuthoriseAsBatchFileUploadCspNrsDisabled[A](implicit vhr: ValidatedHeadersRequest[A]): Future[Either[Result, Option[(BadgeIdentifierEoriPair, Option[RetrievalData])]]] = {
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    authorised(Enrolment("write:customs-declaration") and AuthProviders(PrivilegedApplication)) {
      Future.successful {
        eitherMaybeBadgeIdentifierEoriPair.right.map(maybePair => Some((maybePair.get, None)))
      }
    }.recover{
      case NonFatal(_: AuthorisationException) =>
        logger.debug("Not authorised as CSP")
        Right(None)
      case NonFatal(e) =>
        logger.error("Error authorising CSP", e)
        Left(ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  protected def futureAuthoriseAsBatchFileUploadCspNrsEnabled[A](implicit vhr: ValidatedHeadersRequest[A]): Future[Either[Result, Option[(BadgeIdentifierEoriPair, Option[RetrievalData])]]] = {
    implicit def hc(implicit rh: RequestHeader): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(rh.headers)

    authorised(Enrolment("write:customs-declaration") and AuthProviders(PrivilegedApplication)).retrieve(cspRetrievals) {
      case internalId ~ externalId ~ agentCode ~ confidenceLevel ~ nino ~ saUtr ~ mdtpInformation ~ affinityGroup ~ credentialStrength ~ loginTimes =>
        val retrievalData = CspRetrievalData(internalId, externalId, agentCode, confidenceLevel, nino, saUtr,
          mdtpInformation, affinityGroup, credentialStrength, loginTimes)
        Future.successful {
          eitherMaybeBadgeIdentifierEoriPair.right.map(maybePair => Some((maybePair.get, Some(retrievalData))))
        }
    }.recover{
      case NonFatal(_: AuthorisationException) =>
        logger.debug("Not authorised as CSP")
        Right(None)
      case NonFatal(e) =>
        logger.error("Error authorising CSP", e)
        Left(ErrorInternalServerError.XmlResult.withConversationId)
    }
  }

  private def eitherMaybeBadgeIdentifierEoriPair[A](implicit vhr: ValidatedHeadersRequest[A]): Either[Result, Some[BadgeIdentifierEoriPair]] = {
    for {
      badgeId <- eitherBadgeIdentifierWithValidation.right
      eori <- eitherEoriIdentifierWithValidationCSP.right
    } yield Some(BadgeIdentifierEoriPair(badgeId, eori))
  }

  private def eitherBadgeIdentifierWithValidation[A](implicit vhr: ValidatedHeadersRequest[A]) = {
    val maybeBadgeIdString: Option[String] = maybeHeader(XBadgeIdentifierHeaderName)
    maybeBadgeIdString.filter(xBadgeIdentifierRegex.findFirstIn(_).nonEmpty).map(BadgeIdentifier).fold[Either[Result, BadgeIdentifier]] {
      logger.error("badge identifier invalid or not present for CSP")
      googleAnalyticsConnector.failure(errorResponseBadgeIdentifierHeaderMissing.message)
      Left(errorResponseBadgeIdentifierHeaderMissing.XmlResult.withConversationId)
    } { badgeId: BadgeIdentifier =>
      Right(badgeId)
    }
  }

  private def eitherEoriIdentifierWithValidationCSP[A](implicit vhr: ValidatedHeadersRequest[A]) = {
    val maybeEoriId: Option[String] = maybeHeader(XEoriIdentifierHeaderName)
    maybeValidEori(maybeEoriId).fold[Either[Result, Eori]] {
      logger.error(s"EORI identifier invalid or not present for CSP ($maybeEoriId)")
      googleAnalyticsConnector.failure(errorResponseEoriIdentifierHeaderMissing.message)
      Left(errorResponseEoriIdentifierHeaderMissing.XmlResult.withConversationId)
    } { eori =>
      logger.debug("Authorising as CSP")
      Right(eori)
    }
  }

  private def maybeValidEori(maybeValue: Option[String]) = {
    maybeValue.filter(xEoriIdentifierRegex.findFirstIn(_).nonEmpty).map(Eori.apply)
  }

  private def maybeHeader[A](headerName: String)(implicit vhr: ValidatedHeadersRequest[A]) = {
    vhr.request.headers.toSimpleMap.get(headerName)
  }

}
