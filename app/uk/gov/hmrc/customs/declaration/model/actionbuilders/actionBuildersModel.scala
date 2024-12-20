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

package uk.gov.hmrc.customs.declaration.model.actionbuilders

import play.api.mvc.{Request, Result, WrappedRequest}
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames.*
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.upscan.DocumentType

import java.time.ZonedDateTime
import scala.annotation.unused
import scala.xml.NodeSeq

object ActionBuilderModelHelper {

  implicit class AddConversationId(val result: Result) extends AnyVal {
    def withConversationId(implicit c: HasConversationId): Result = {
      result.withHeaders(XConversationIdHeaderName -> c.conversationId.toString)
    }
  }

  implicit class AddNrSubmissionId(val result: Result) extends AnyVal {
    def withNrSubmissionId(n: NrSubmissionId): Result = {
      result.withHeaders(NonRepudiationReceiptId -> n.toString)
    }
  }

  implicit class ConversationIdRequestOps[A](val cir: ConversationIdRequest[A]) extends AnyVal {
    def toApiVersionRequest(apiVersion: ApiVersion): ApiVersionRequest[A] = ApiVersionRequest(
      cir.conversationId,
      cir.start,
      apiVersion,
      cir.request
    )
  }

  implicit class ApiVersionRequestOps[A](val avr: ApiVersionRequest[A]) extends AnyVal {
    def toValidatedHeadersRequest(eh: ExtractedHeaders): ValidatedHeadersRequest[A] = ValidatedHeadersRequest(
      avr.conversationId,
      avr.start,
      avr.requestedApiVersion,
      eh.clientId,
      avr.request
    )

    def toValidatedHeadersStatusRequest(eh: ExtractedStatusHeaders): ValidatedHeadersStatusRequest[A] = ValidatedHeadersStatusRequest(
      avr.conversationId,
      avr.start,
      avr.requestedApiVersion,
      eh.badgeIdentifier,
      eh.clientId,
      avr.request
    )
  }

  implicit class ValidatedHeadersRequestOps[A](val vhr: ValidatedHeadersRequest[A]) {

    def toCspAuthorisedRequest(a: AuthorisedAsCsp): AuthorisedRequest[A] = toAuthorisedRequest(a)

    def toNonCspAuthorisedRequest(eori: Eori, retrievalData: Option[NrsRetrievalData]): AuthorisedRequest[A] = toAuthorisedRequest(NonCsp(eori, retrievalData))

    private def toAuthorisedRequest(authorisedAs: AuthorisedAs): AuthorisedRequest[A] = AuthorisedRequest(
      vhr.conversationId,
      vhr.start,
      vhr.requestedApiVersion,
      vhr.clientId,
      authorisedAs,
      vhr.request
    )
  }

  implicit class ValidatedHeadersStatusRequestOps[A](val vhsr: ValidatedHeadersStatusRequest[A]) extends AnyVal {

    def toAuthorisedRequest(authorisedAsCsp: AuthorisedAsCsp): AuthorisedRequest[A] = AuthorisedRequest(
      vhsr.conversationId,
      vhsr.start,
      vhsr.requestedApiVersion,
      vhsr.clientId,
      authorisedAsCsp,
      vhsr.request
    )
  }

  implicit class AuthorisedRequestOps[A](val ar: AuthorisedRequest[A]) extends AnyVal {
    def toValidatedPayloadRequest(xmlBody: NodeSeq): ValidatedPayloadRequest[A] = ValidatedPayloadRequest(
      ar.conversationId,
      ar.start,
      ar.requestedApiVersion,
      ar.clientId,
      ar.authorisedAs,
      xmlBody,
      ar.request
    )
  }

  implicit class ValidatedPayloadRequestOps[A](val vpr: ValidatedPayloadRequest[A]) extends AnyVal {

    def toValidatedFileUploadPayloadRequest(fileUploadRequest: FileUploadRequest): ValidatedFileUploadPayloadRequest[A] =
      ValidatedFileUploadPayloadRequest(
        vpr.conversationId,
        vpr.start,
        vpr.requestedApiVersion,
        vpr.clientId,
        vpr.authorisedAs,
        vpr.xmlBody,
        vpr.request,
        fileUploadRequest
      )
  }

}

trait HasRequest[A] {
  val request: Request[A]
}

trait HasConversationId {
  val conversationId: ConversationId
}

trait HasApiVersion {
  val requestedApiVersion: ApiVersion
}

trait ExtractedHeaders {
  val clientId: ClientId
}

trait HasAuthorisedAs {
  val authorisedAs: AuthorisedAs
}

trait HasXmlBody {
  val xmlBody: NodeSeq
}

case class FileUploadRequest(declarationId: DeclarationId, fileGroupSize: FileGroupSize, files: Seq[FileUploadFile])

case class FileUploadFile(fileSequenceNo: FileSequenceNo, maybeDocumentType: Option[DocumentType], successRedirect: Option[String], errorRedirect: Option[String]) {

  def canEqual(a: Any): Boolean = a.isInstanceOf[FileUploadFile]

  override def equals(that: Any): Boolean =
    that match {
      case that: FileUploadFile => that.canEqual(this) && this.hashCode == that.hashCode
      case _ => false
    }
  override def hashCode: Int = {
    fileSequenceNo.value
  }
}

trait HasFileUploadProperties {
  val fileUploadRequest: FileUploadRequest
}

trait HasBadgeIdentifier {
  val badgeIdentifier: BadgeIdentifier
}

case class ExtractedHeadersImpl(clientId: ClientId
) extends ExtractedHeaders

trait ExtractedStatusHeaders extends ExtractedHeaders {
  val badgeIdentifier: BadgeIdentifier
}

case class ExtractedStatusHeadersImpl(badgeIdentifier: BadgeIdentifier,
                                      clientId: ClientId
) extends ExtractedStatusHeaders

/*
 * We need multiple WrappedRequest classes to reflect additions to context during the request processing pipeline.
 *
 * There is some repetition in the WrappedRequest classes, but the benefit is we get a flat structure for our data
 * items, reducing the number of case classes and making their use much more convenient, rather than deeply nested stuff
 * eg `r.badgeIdentifier` vs `r.requestData.badgeIdentifier`
 */

case class ConversationIdRequest[A](conversationId: ConversationId,
                                    start: ZonedDateTime,
                                    request: Request[A]
) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId

// Available after ShutterCheckAction
case class ApiVersionRequest[A](conversationId: ConversationId,
                                start: ZonedDateTime,
                                requestedApiVersion: ApiVersion,
                                request: Request[A]
) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with HasApiVersion

// Available after ValidatedHeadersAction builder
case class ValidatedHeadersRequest[A](conversationId: ConversationId,
                                      start: ZonedDateTime,
                                      requestedApiVersion: ApiVersion,
                                      clientId: ClientId,
                                      request: Request[A]
) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with HasApiVersion with ExtractedHeaders

// Specifically for status endpoint
case class ValidatedHeadersStatusRequest[A](conversationId: ConversationId,
                                            start: ZonedDateTime,
                                            requestedApiVersion: ApiVersion,
                                            badgeIdentifier: BadgeIdentifier,
                                            clientId: ClientId,
                                            request: Request[A]
) extends WrappedRequest[A](request) with HasRequest[A] with HasConversationId with HasApiVersion with HasBadgeIdentifier with ExtractedStatusHeaders

// Available after AuthAction builder
case class AuthorisedRequest[A](conversationId: ConversationId,
                                start: ZonedDateTime,
                                requestedApiVersion: ApiVersion,
                                clientId: ClientId,
                                authorisedAs: AuthorisedAs,
                                request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with HasApiVersion with ExtractedHeaders with HasAuthorisedAs

// Available after ValidatedPayloadAction builder
abstract class GenericValidatedPayloadRequest[A](@unused conversationId: ConversationId,
                                                 @unused start: ZonedDateTime,
                                                 @unused requestedApiVersion: ApiVersion,
                                                 @unused clientId: ClientId,
                                                 @unused authorisedAs: AuthorisedAs,
                                                 @unused xmlBody: NodeSeq,
                                                 request: Request[A]
) extends WrappedRequest[A](request) with HasConversationId with HasApiVersion with ExtractedHeaders with HasAuthorisedAs with HasXmlBody

case class ValidatedPayloadRequest[A](conversationId: ConversationId,
                                      start: ZonedDateTime,
                                      requestedApiVersion: ApiVersion,
                                      clientId: ClientId,
                                      authorisedAs: AuthorisedAs,
                                      xmlBody: NodeSeq,
                                      request: Request[A]
) extends GenericValidatedPayloadRequest(conversationId, start, requestedApiVersion, clientId, authorisedAs, xmlBody, request)

case class ValidatedFileUploadPayloadRequest[A](conversationId: ConversationId,
                                                start: ZonedDateTime,
                                                requestedApiVersion: ApiVersion,
                                                clientId: ClientId,
                                                authorisedAs: AuthorisedAs,
                                                xmlBody: NodeSeq,
                                                request: Request[A],
                                                fileUploadRequest: FileUploadRequest
) extends GenericValidatedPayloadRequest(conversationId,
                                         start,
                                         requestedApiVersion,
                                         clientId,
                                         authorisedAs,
                                         xmlBody,
                                         request) with HasFileUploadProperties
