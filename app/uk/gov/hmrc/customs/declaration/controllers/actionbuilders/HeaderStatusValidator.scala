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

import play.api.mvc.Headers
import play.mvc.Http.Status.BAD_REQUEST
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._

@Singleton
class HeaderStatusValidator @Inject()(logger: DeclarationsLogger) extends HeaderValidator(logger) {

  private lazy val xBadgeIdentifierRegex = "^[0-9A-Z]{6,12}$".r

  // Not V1
  override val versionsByAcceptHeader: Map[String, ApiVersion] = Map(
    "application/vnd.hmrc.2.0+xml" -> VersionTwo,
    "application/vnd.hmrc.3.0+xml" -> VersionThree
  )

  override def validateHeaders[A](implicit conversationIdRequest: AnalyticsValuesAndConversationIdRequest[A]): Either[ErrorResponse, ExtractedStatusHeaders] = {

    implicit val headers: Headers = conversationIdRequest.headers

    def hasBadgeIdentifier = validateHeader(XBadgeIdentifierHeaderName, xBadgeIdentifierRegex.findFirstIn(_).nonEmpty, ErrorResponse(BAD_REQUEST, BadRequestCode, s"$XBadgeIdentifierHeaderName header is missing or invalid"))

    super.validateHeaders match {
      case Right(b) =>
        val theResult: Either[ErrorResponse, ExtractedStatusHeaders] = for {
        badgeIdentifier <- hasBadgeIdentifier.right
        } yield {
          logger.debug(s"$XBadgeIdentifierHeaderName header passed validation: $badgeIdentifier")
          ExtractedStatusHeadersImpl(b.requestedApiVersion, BadgeIdentifier(badgeIdentifier), b.clientId)
        }
        theResult
      case Left(a) => Left(a)
    }
  }
}

