/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.Headers
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._

import javax.inject.Inject
import scala.util.matching.Regex

abstract class HeaderValidator @Inject()(logger: DeclarationsLogger) {

  private lazy val xClientIdRegex: Regex = "^\\S+$".r
  private lazy val xBadgeIdentifierRegex: Regex = "^[0-9A-Z]{6,12}$".r
  private lazy val InvalidEoriHeaderRegex: Regex = "(^[\\s]*$|^.{18,}$)".r

  private val errorResponseBadgeIdentifierHeaderMissing = errorBadRequest(s"$XBadgeIdentifierHeaderName header is missing or invalid")
  private def errorResponseEoriIdentifierHeaderMissingOrInvalid(eoriHeaderName: String) = errorBadRequest(s"$eoriHeaderName header is missing or invalid")
  private def errorResponseEoriIdentifierHeaderInvalid(eoriHeaderName: String) = errorBadRequest(s"$eoriHeaderName header is invalid")

  def validateHeaders[A](implicit apiVersionRequest: ApiVersionRequest[A]): Either[ErrorResponse, ExtractedHeaders] = {
    implicit val headers: Headers = apiVersionRequest.headers

    def hasXClientId = validateHeader(XClientIdHeaderName, xClientIdRegex.findFirstIn(_).nonEmpty, ErrorInternalServerError)

    val theResult: Either[ErrorResponse, ExtractedHeaders] = for {
      xClientIdValue <- hasXClientId
    } yield {
      val clientId = ClientId(xClientIdValue)
      ExtractedHeadersImpl(clientId)
    }
    theResult
  }

  def logAcceptAndClientIdHeaderText[A](clientId: ClientId): String = {
    s"\n$XClientIdHeaderName header passed validation: ${clientId.value}"
  }

  protected def validateHeader[A](headerName: String, rule: String => Boolean, errorResponse: ErrorResponse)
                                 (implicit conversationIdRequest: ApiVersionRequest[A], h: Headers): Either[ErrorResponse, String] = {
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

  def eitherBadgeIdentifier[A](allowNone: Boolean)(implicit vhr: HasRequest[A] & HasConversationId): Either[ErrorResponse, Option[BadgeIdentifier]] = {
    val maybeBadgeId: Option[String] = vhr.request.headers.toSimpleMap.get(XBadgeIdentifierHeaderName)

    if (allowNone && maybeBadgeId.isEmpty) {
      Right(None)
    } else {
      maybeBadgeId.filter(xBadgeIdentifierRegex.findFirstIn(_).nonEmpty).map(b => {
        Some(BadgeIdentifier(b))
      }
      ).toRight[ErrorResponse] {
        logger.error(s"$XBadgeIdentifierHeaderName invalid or not present for CSP")
        errorResponseBadgeIdentifierHeaderMissing
      }
    }
  }

  def eoriMustBeValidAndPresent[A](eoriHeaderName: String)(implicit vhr: HasRequest[A] & HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    val maybeEori: Option[String] = vhr.request.headers.toSimpleMap.get(eoriHeaderName)

    maybeEori.filter(validEori).map(e =>
      Some(Eori(e))
    ).toRight{
      logger.error(s"$eoriHeaderName header is invalid or not present for CSP: $maybeEori")
      errorResponseEoriIdentifierHeaderMissingOrInvalid(eoriHeaderName)
    }
  }

  private def validEori(eori: String) = InvalidEoriHeaderRegex.findFirstIn(eori).isEmpty

  private def convertEmptyHeaderToNone(eori: Option[String]) = {
    if (eori.isDefined && eori.get.trim.isEmpty) {
      eori map (_.trim) filterNot (_.isEmpty)
    } else {
      eori
    }
  }

  def eoriMustBeValidIfPresent[A](eoriHeaderName: String)(implicit vhr: HasRequest[A] & HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    val maybeEoriHeader: Option[String] = vhr.request.headers.toSimpleMap.get(eoriHeaderName)

    val maybeEori = convertEmptyHeaderToNone(maybeEoriHeader)

    maybeEori match {
      case Some(eori) =>
        if (validEori(eori)) {
          Right(Some(Eori(eori)))
        } else {
          logger.error(s"$eoriHeaderName header is invalid for CSP: $eori")
          Left(errorResponseEoriIdentifierHeaderInvalid(eoriHeaderName))
        }
      case None =>
        Right(None)
    }
  }

  def logEoriAndBadgeIdHeaderText[A](eoriHeaderName: String, maybeBadgeId: Option[BadgeIdentifier], maybeEori: Option[Eori]): String = {
      s"${logEoriHeaderText(eoriHeaderName, maybeEori)}" +
      s"\n${logBadgeIdHeaderText(maybeBadgeId)}"
  }

  def logEoriHeaderText[A](eoriHeaderName: String, maybeEori: Option[Eori]): String = {
    maybeEori match {
      case None => s"$eoriHeaderName header not present or is empty"
      case Some(eori) => s"$eoriHeaderName header passed validation: $eori"
    }
  }

  def logBadgeIdHeaderText[A](maybeBadgeId: Option[BadgeIdentifier]): String = {
    maybeBadgeId match {
      case None => s"$XBadgeIdentifierHeaderName header empty and allowed"
      case Some(badgeId) => s"$XBadgeIdentifierHeaderName header passed validation: $badgeId"
    }
  }
}
