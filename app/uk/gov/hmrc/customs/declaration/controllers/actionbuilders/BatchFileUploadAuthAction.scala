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
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedHeadersRequest
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}

@Singleton
class BatchFileUploadAuthAction @Inject()(customsAuthService: CustomsAuthService,
                                          logger: DeclarationsLogger,
                                          googleAnalyticsConnector: GoogleAnalyticsConnector,
                                          declarationConfigService: DeclarationsConfigService)
  extends AuthAction(customsAuthService, logger, googleAnalyticsConnector, declarationConfigService) {

  private lazy val xEoriIdentifierRegex = "^[0-9A-Za-z]{1,17}$".r
  private val errorResponseEoriIdentifierHeaderMissing = errorBadRequest(s"$XEoriIdentifierHeaderName header is missing or invalid")

  override def eitherCspAuthData[A](maybeNrsRetrievalData: Option[NrsRetrievalData])(implicit vhr: ValidatedHeadersRequest[A]): Either[ErrorResponse, AuthorisedAsCsp] = {
    for {
      badgeId <- eitherBadgeIdentifier.right
      eori <- maybeEori.right
    } yield BatchFileUploadCsp(badgeId, eori, maybeNrsRetrievalData)
  }

  private def maybeEori[A](implicit vhr: ValidatedHeadersRequest[A]): Either[ErrorResponse, Eori] = {
    val maybeEori: Option[String] = vhr.request.headers.toSimpleMap.get(XEoriIdentifierHeaderName)

    maybeEori.filter(xEoriIdentifierRegex.findFirstIn(_).nonEmpty).map(s => Eori(s)).toRight{
      logger.error(s"EORI identifier invalid or not present for CSP ($maybeEori)")
      googleAnalyticsConnector.failure(errorResponseEoriIdentifierHeaderMissing.message)
      errorResponseEoriIdentifierHeaderMissing
    }
  }

}
