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

package uk.gov.hmrc.customs.declaration.model.actionbuilders

import play.api.mvc.{Request, Result, WrappedRequest}
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.model.AuthorisedAs.AuthorisedAs
import uk.gov.hmrc.customs.declaration.model._

import scala.xml.NodeSeq

object ActionBuilderModelHelper {

  implicit class AddConversationId(result: Result) {
    def withConversationId(implicit c: HasConversationId): Result = {
      result.withHeaders(XConversationIdHeaderName -> c.conversationId.value)
    }
  }

  implicit class CorrelationIdsRequestOps[A](cir: ConversationIdRequest[A]) {
    def toValidatedHeadersRequest(eh: ExtractedHeaders): ValidatedHeadersRequest[A] = ValidatedHeadersRequest(
      cir.conversationId,
      eh.maybeBadgeIdentifier,
      eh.requestedApiVersion,
      eh.clientId,
      cir.request
    )
  }

  implicit class ValidatedHeadersRequestOps[A](vhr: ValidatedHeadersRequest[A]) {
    def toAuthorisedRequest(maybeAuthorised: Option[AuthorisedAs] = None): AuthorisedRequest[A] = AuthorisedRequest(
        vhr.conversationId,
        vhr.maybeBadgeIdentifier,
        vhr.requestedApiVersion,
        vhr.clientId,
        maybeAuthorised,
        vhr.request
      )
  }

  implicit class AuthorisedRequestOps[A](ar: AuthorisedRequest[A]) {
    def toValidatedPayloadRequest(xmlBody: NodeSeq): ValidatedPayloadRequest[A] = ValidatedPayloadRequest(
        ar.conversationId,
        ar.maybeBadgeIdentifier,
        ar.requestedApiVersion,
        ar.clientId,
        ar.maybeAuthorised,
        xmlBody,
        ar.request
      )
  }

  implicit class AuthorisedRequestTransformationOp[A](ar: AuthorisedRequest[A]) {

    // we can not use normal case class copy on a wrapped request as it is overridden by RequestHeader
    def asNonCsp: AuthorisedRequest[A] = authorisedRequest(Some(AuthorisedAs.NonCsp))

    // we can not use normal case class copy on a wrapped request as it is overridden by RequestHeader
    def asCsp: AuthorisedRequest[A] = authorisedRequest(Some(AuthorisedAs.Csp))

    def authorisedRequest(maybeAuthorised: Option[AuthorisedAs]): AuthorisedRequest[A] = {
      AuthorisedRequest(
        ar.conversationId,
        ar.maybeBadgeIdentifier,
        ar.requestedApiVersion,
        ar.clientId,
        maybeAuthorised,
        ar.request
      )
    }
  }

}

trait HasConversationId {
  val conversationId: ConversationId
}

trait ExtractedHeaders {
  val maybeBadgeIdentifier: Option[BadgeIdentifier]
  val requestedApiVersion: ApiVersion
  val clientId: ClientId
}

trait HasAuthorisedAs {
  val maybeAuthorised: Option[AuthorisedAs]
}

trait HasXmlBody {
  val xmlBody: NodeSeq
}

case class ExtractedHeadersImpl(
  maybeBadgeIdentifier: Option[BadgeIdentifier],
  requestedApiVersion: ApiVersion,
  clientId: ClientId
) extends ExtractedHeaders

/*
 * We need multiple WrappedRequest classes to reflect additions to context during the request processing pipeline.
 *
 * There is some repetition in the WrappedRequest classes, but the benefit is we get a flat structure for our data
 * items, reducing the number of case classes and making their use much more convenient, rather than deeply nested stuff
 * eg `r.badgeIdentifier` vs `r.requestData.badgeIdentifier`
 */

// Available after ConversationIdAction action builder
case class ConversationIdRequest[A](
  conversationId: ConversationId,
  request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId

// Available after ValidatedHeadersAction builder
case class ValidatedHeadersRequest[A](
  conversationId: ConversationId,
  maybeBadgeIdentifier: Option[BadgeIdentifier],
  requestedApiVersion: ApiVersion,
  clientId: ClientId,
  request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders

// Available after ValidatedHeadersAction builder
case class AuthorisedRequest[A](
                                 conversationId: ConversationId,
                                 maybeBadgeIdentifier: Option[BadgeIdentifier],
                                 requestedApiVersion: ApiVersion,
                                 clientId: ClientId,
                                 maybeAuthorised: Option[AuthorisedAs] = None,
                                 request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders with HasAuthorisedAs

// Available after ValidatedPayloadAction builder
case class ValidatedPayloadRequest[A](
  conversationId: ConversationId,
  maybeBadgeIdentifier: Option[BadgeIdentifier],
  requestedApiVersion: ApiVersion,
  clientId: ClientId,
  maybeAuthorised: Option[AuthorisedAs] = None,
  xmlBody: NodeSeq,
  request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with ExtractedHeaders with HasAuthorisedAs with HasXmlBody
