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
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{HasAnalyticsValues, HasConversationId, HasRequest}
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}

@Singleton
class BatchFileUploadAuthAction @Inject()(customsAuthService: CustomsAuthService,
                                          headerValidator: HeaderValidator,
                                          logger: DeclarationsLogger,
                                          googleAnalyticsConnector: GoogleAnalyticsConnector,
                                          declarationConfigService: DeclarationsConfigService)
  extends AuthAction(customsAuthService, headerValidator, logger, googleAnalyticsConnector, declarationConfigService) {

  override def eitherCspAuthData[A](maybeNrsRetrievalData: Option[NrsRetrievalData])(implicit vhr: HasRequest[A] with HasConversationId with HasAnalyticsValues): Either[ErrorResponse, AuthorisedAsCsp] = {
    for {
      badgeId <- eitherBadgeIdentifier.right
      eori <- eitherEori.right
    } yield BatchFileUploadCsp(badgeId, eori, maybeNrsRetrievalData)
  }

  private def eitherEori[A](implicit vhr: HasRequest[A] with HasConversationId with HasAnalyticsValues): Either[ErrorResponse, Eori] = {
    headerValidator.eitherEori(XEoriIdentifierHeaderName).left.map{errorResponse =>
      googleAnalyticsConnector.failure(errorResponse.message)
      errorResponse
    }
  }

}
