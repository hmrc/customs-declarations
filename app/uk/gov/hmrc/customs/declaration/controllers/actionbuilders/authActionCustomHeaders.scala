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
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}

import scala.concurrent.ExecutionContext

abstract class AuthActionCustomHeader @Inject()(customsAuthService: CustomsAuthService,
                                                headerValidator: HeaderWithContentTypeValidator,
                                                logger: DeclarationsLogger,
                                                declarationConfigService: DeclarationsConfigService,
                                                eoriHeaderName: String)
                                               (implicit ec: ExecutionContext)
  extends AuthAction(customsAuthService, headerValidator, logger, declarationConfigService) {

  override def eitherCspAuthData[A](maybeNrsRetrievalData: Option[NrsRetrievalData])(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, AuthorisedAsCsp] = {
    for {
      maybeBadgeId <- eitherBadgeIdentifier(allowNone = false).right
      maybeEori <- eitherEori.right
    } yield Csp(maybeEori, maybeBadgeId, maybeNrsRetrievalData)
  }

  private def eitherEori[A](implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    headerValidator.eoriMustBeValidAndPresent(eoriHeaderName)
  }

}

@Singleton
class AuthActionEoriHeader @Inject()(customsAuthService: CustomsAuthService,
                                     headerValidator: HeaderWithContentTypeValidator,
                                     logger: DeclarationsLogger,
                                     declarationConfigService: DeclarationsConfigService)
                                    (implicit ec: ExecutionContext)
  extends AuthActionCustomHeader(customsAuthService, headerValidator, logger, declarationConfigService, XEoriIdentifierHeaderName) {
  override def requestRetrievalsForEndpoint: Boolean = false
  override def executionContext: ExecutionContext = ec
}


@Singleton
class AuthActionStrictSubmitterHeader @Inject()(customsAuthService: CustomsAuthService,
                                                headerValidator: HeaderWithContentTypeValidator,
                                                logger: DeclarationsLogger,
                                                declarationConfigService: DeclarationsConfigService)
                                               (implicit ec: ExecutionContext)
  extends AuthActionCustomHeader(customsAuthService, headerValidator, logger, declarationConfigService, XSubmitterIdentifierHeaderName) {

  private def errorResponseMissingIdentifiers = errorBadRequest(s"Both $XSubmitterIdentifierHeaderName and $XBadgeIdentifierHeaderName headers are missing")

  override def eitherCspAuthData[A](maybeNrsRetrievalData: Option[NrsRetrievalData])(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, AuthorisedAsCsp] = {
    val cspAuth: Either[ErrorResponse, Csp] = for {
      maybeBadgeId <- eitherBadgeIdentifier(allowNone = true).right
      maybeEori <- eitherEori.right
    } yield Csp(maybeEori, maybeBadgeId, maybeNrsRetrievalData)

    rejectEmptyIdentifierHeaders(cspAuth)
  }

  private def eitherEori[A](implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    headerValidator.eoriMustBeValidIfPresent(XSubmitterIdentifierHeaderName)
  }

  protected def rejectEmptyIdentifierHeaders[A](cspAuth: Either[ErrorResponse, Csp])(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Csp] = {
    if (cspAuth.isRight && cspAuth.right.get.badgeIdentifier.isEmpty && cspAuth.right.get.eori.isEmpty) {
      logger.error(s"Both $XSubmitterIdentifierHeaderName and $XBadgeIdentifierHeaderName headers are missing")
      Left(errorResponseMissingIdentifiers)
    } else {
      cspAuth
    }
  }
}

class AuthActionRelaxedSubmitterHeader @Inject()(customsAuthService: CustomsAuthService,
                                                 headerValidator: HeaderWithContentTypeValidator,
                                                 logger: DeclarationsLogger,
                                                 declarationConfigService: DeclarationsConfigService)
                                                (implicit ec: ExecutionContext)
  extends AuthActionStrictSubmitterHeader(customsAuthService, headerValidator, logger, declarationConfigService) {

  override protected def rejectEmptyIdentifierHeaders[A](cspAuth: Either[ErrorResponse, Csp])(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Csp] = {
    logger.debug("skip check and allow empty identifier headers")
    cspAuth
  }

}
