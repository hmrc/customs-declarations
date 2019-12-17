/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.http.HeaderNames._
import play.api.mvc.Headers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._

import scala.util.matching.Regex

abstract class HeaderValidator @Inject()(logger: DeclarationsLogger) {

  protected val versionsByAcceptHeader: Map[String, ApiVersion] = Map(
    "application/vnd.hmrc.1.0+xml" -> VersionOne,
    "application/vnd.hmrc.2.0+xml" -> VersionTwo,
    "application/vnd.hmrc.3.0+xml" -> VersionThree
  )

  private lazy val xClientIdRegex: Regex = "^\\S+$".r
  private lazy val xBadgeIdentifierRegex: Regex = "^[0-9A-Z]{6,12}$".r
  private lazy val InvalidEoriHeaderRegex: Regex = "(^[\\s]*$|^.{18,}$)".r

  private val errorResponseBadgeIdentifierHeaderMissing = errorBadRequest(s"$XBadgeIdentifierHeaderName header is missing or invalid")
  private def errorResponseEoriIdentifierHeaderMissingOrInvalid(eoriHeaderName: String) = errorBadRequest(s"$eoriHeaderName header is missing or invalid")
  private def errorResponseEoriIdentifierHeaderInvalid(eoriHeaderName: String) = errorBadRequest(s"$eoriHeaderName header is invalid")

  def validateHeaders[A](implicit conversationIdRequest: ConversationIdRequest[A]): Either[ErrorResponse, ExtractedHeaders] = {
    implicit val headers: Headers = conversationIdRequest.headers

    def hasAccept = validateHeader(ACCEPT, versionsByAcceptHeader.keySet.contains(_), ErrorAcceptHeaderInvalid)

    def hasXClientId = validateHeader(XClientIdHeaderName, xClientIdRegex.findFirstIn(_).nonEmpty, ErrorInternalServerError)

    val theResult: Either[ErrorResponse, ExtractedHeaders] = for {
      acceptValue <- hasAccept.right
      xClientIdValue <- hasXClientId.right
    } yield {
      logger.debug(
        s"\n$ACCEPT header passed validation: $acceptValue"
      + s"\n$XClientIdHeaderName header passed validation: $xClientIdValue")
      ExtractedHeadersImpl(versionsByAcceptHeader(acceptValue), ClientId(xClientIdValue))
    }
    theResult
  }

  protected def validateHeader[A](headerName: String, rule: String => Boolean, errorResponse: ErrorResponse)
                               (implicit conversationIdRequest: ConversationIdRequest[A], h: Headers): Either[ErrorResponse, String] = {
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

  def eitherBadgeIdentifier[A](allowNone: Boolean)(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[BadgeIdentifier]] = {
    val maybeBadgeId: Option[String] = vhr.request.headers.toSimpleMap.get(XBadgeIdentifierHeaderName)

    if (allowNone && maybeBadgeId.isEmpty) {
      logger.info(s"$XBadgeIdentifierHeaderName header empty and allowed")
      Right(None)
    } else {
      maybeBadgeId.filter(xBadgeIdentifierRegex.findFirstIn(_).nonEmpty).map(b => {
        logger.info(s"$XBadgeIdentifierHeaderName header passed validation: $b")
        Some(BadgeIdentifier(b))
      }
      ).toRight[ErrorResponse] {
        logger.error(s"$XBadgeIdentifierHeaderName invalid or not present for CSP")
        errorResponseBadgeIdentifierHeaderMissing
      }
    }
  }

  def eoriMustBeValidAndPresent[A](eoriHeaderName: String)(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    val maybeEori: Option[String] = vhr.request.headers.toSimpleMap.get(eoriHeaderName)

    maybeEori.filter(InvalidEoriHeaderRegex.findFirstIn(_).isEmpty).map(e =>
      {
        logger.info(s"$eoriHeaderName header passed validation: $e")
        Some(Eori(e))
      }
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

  def eoriMustBeValidIfPresent[A](eoriHeaderName: String)(implicit vhr: HasRequest[A] with HasConversationId): Either[ErrorResponse, Option[Eori]] = {
    val maybeEoriHeader: Option[String] = vhr.request.headers.toSimpleMap.get(eoriHeaderName)
    logger.debug(s"maybeEori => $maybeEoriHeader")
    val maybeEori = convertEmptyHeaderToNone(maybeEoriHeader)

    maybeEori match {
      case Some(eori) => if (validEori(eori)) {
        logger.info(s"$eoriHeaderName header passed validation: $eori")
        Right(Some(Eori(eori)))
      } else {
        logger.error(s"$eoriHeaderName header is invalid for CSP: $eori")
        Left(errorResponseEoriIdentifierHeaderInvalid(eoriHeaderName))
      }
      case None =>
        logger.info(s"$eoriHeaderName header not present or is empty")
        Right(None)
    }
  }
}

