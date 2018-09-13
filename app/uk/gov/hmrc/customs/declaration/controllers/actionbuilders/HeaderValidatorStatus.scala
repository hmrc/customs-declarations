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

import play.api.http.HeaderNames._
import play.api.mvc.Headers
import play.mvc.Http.Status.BAD_REQUEST
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AnalyticsValuesAndConversationIdRequest, ExtractedStatusHeaders, ExtractedStatusHeadersImpl}

@Singleton
class HeaderValidatorStatus @Inject()(logger: DeclarationsLogger) {

  private val versionsByAcceptHeader: Map[String, ApiVersion] = Map(
    "application/vnd.hmrc.1.0+xml" -> VersionOne,
    "application/vnd.hmrc.2.0+xml" -> VersionTwo,
    "application/vnd.hmrc.3.0+xml" -> VersionThree
  )

  private lazy val xBadgeIdentifierRegex = "^[0-9A-Z]{6,12}$".r
  private lazy val xClientIdRegex = "^\\S+$".r

  def validateHeaders[A](implicit conversationIdRequest: AnalyticsValuesAndConversationIdRequest[A]): Either[ErrorResponse, ExtractedStatusHeaders] = {
    implicit val headers: Headers = conversationIdRequest.headers

    def hasAccept = validateHeader(ACCEPT, versionsByAcceptHeader.keySet.contains(_), ErrorAcceptHeaderInvalid)

    def hasBadgeIdentifier = validateHeader(XBadgeIdentifierHeaderName, xBadgeIdentifierRegex.findFirstIn(_).nonEmpty, ErrorResponse(BAD_REQUEST, BadRequestCode, s"Invalid or missing $XBadgeIdentifierHeaderName header"))

    def hasXClientId = validateHeader(XClientIdHeaderName, xClientIdRegex.findFirstIn(_).nonEmpty, ErrorInternalServerError)

    val theResult: Either[ErrorResponse, ExtractedStatusHeaders] = for {
      acceptValue <- hasAccept.right
      badgeIdentifier <- hasBadgeIdentifier.right
      xClientIdValue <- hasXClientId.right
    } yield {
      logger.debug(
        s"\n$ACCEPT header passed validation: $acceptValue"
      + s"\n$XBadgeIdentifierHeaderName header passed validation: $badgeIdentifier"
      + s"\n$XClientIdHeaderName header passed validation: $xClientIdValue")

      ExtractedStatusHeadersImpl(versionsByAcceptHeader(acceptValue), BadgeIdentifier(badgeIdentifier), ClientId(xClientIdValue))
    }
    theResult
  }

  private def validateHeader[A](headerName: String, rule: String => Boolean, errorResponse: ErrorResponse)
                               (implicit conversationIdRequest: AnalyticsValuesAndConversationIdRequest[A], h: Headers): Either[ErrorResponse, String] = {
    val left = Left(errorResponse)
    def leftWithLog(headerName: String) = {
      logger.error(s"Error - header '$headerName' not present")
      left
    }
    def leftWithLogContainingValue(headerName: String, value: String) = {
      logger.error(s"Error - header '$headerName' value '$value' is not valid")
      left
    }

    h.get(headerName).fold[Either[ErrorResponse, String]]{
      leftWithLog(headerName)
    }{
      v =>
        if (rule(v)) Right(v) else leftWithLogContainingValue(headerName, v)
    }
  }

}

