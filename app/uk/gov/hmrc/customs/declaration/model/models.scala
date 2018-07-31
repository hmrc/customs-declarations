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

package uk.gov.hmrc.customs.declaration.model

import java.util.UUID

import org.joda.time.LocalDate
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.auth.core.retrieve.AgentInformation
import uk.gov.hmrc.auth.core.retrieve.MdtpInformation
import uk.gov.hmrc.auth.core.retrieve.ItmpName
import uk.gov.hmrc.auth.core.retrieve.ItmpAddress
import uk.gov.hmrc.auth.core.retrieve.LoginTimes

import scala.xml.{Elem, NodeSeq}

case class RequestedVersion(versionNumber: String, configPrefix: Option[String])

case class Eori(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class NrSubmissionId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}

object NrSubmissionId {
  implicit val format = Json.format[NrSubmissionId]
}

case class NrsRetrievalData(internalId: Option[String],
                            externalId: Option[String],
                            agentCode: Option[String],
                            credentials: Credentials,
                            confidenceLevel: ConfidenceLevel,
                            nino: Option[String],
                            saUtr: Option[String],
                            name: Name,
                            dateOfBirth: Option[LocalDate],
                            email: Option[String],
                            agentInformation: AgentInformation,
                            groupIdentifier: Option[String],
                            credentialRole: Option[CredentialRole],
                            mdtpInformation: Option[MdtpInformation],
                            itmpName: ItmpName,
                            itmpDateOfBirth: Option[LocalDate],
                            itmpAddress: ItmpAddress,
                            affinityGroup: Option[AffinityGroup],
                            credentialStrength: Option[String],
                            loginTimes: LoginTimes)

object NrsRetrievalData {
  implicit val credentialsFormat = Json.format[Credentials]

  implicit val nameFormat = Json.format[Name]

  implicit val agentInformationFormat = Json.format[AgentInformation]

  implicit val mdtpInformationFormat = Json.format[MdtpInformation]

  implicit val itmpNameFormat = Json.format[ItmpName]

  implicit val itmpAddressFormat = Json.format[ItmpAddress]

  implicit val loginTimesFormat = Json.format[LoginTimes]

  implicit val nrsRetrievalDataFormat = Json.format[NrsRetrievalData]
}

case class ClientId(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class ConversationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

sealed trait GoogleAnalyticsValues {
  val success: String
  val failure: String
}

object GoogleAnalyticsValues {
  val Submit: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override val success: String = "declarationSubmitSuccess"
    override val failure: String = "declarationSubmitFailure"
  }

  val Cancel: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override val success: String = "declarationCancellationSuccess"
    override val failure: String = "declarationCancellationFailure"
  }

  val Fileupload = new GoogleAnalyticsValues {
    override val success: String = "declarationFileUploadSuccess"
    override val failure: String = "declarationFileUploadFailure"
  }

  val Clearance = new GoogleAnalyticsValues {
    override val success: String = "declarationClearanceSuccess"
    override val failure: String = "declarationClearanceFailure"
  }

  //according to the ticket, amend endpoint should not call GA
  //hence, to be on a safe side exception might be in order
  val Amend = new GoogleAnalyticsValues {
    override lazy val success: String = ???
    override lazy val failure: String = ???
  }
}


case class CorrelationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

case class BadgeIdentifier(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class SubscriptionFieldsId(value: String) extends AnyVal{
  override def toString: String = value.toString
}

case class DeclarationId(value: String) extends AnyVal{
  override def toString: String = value.toString
}

case class DocumentationType(value: String) extends AnyVal{
  override def toString: String = value.toString
}

sealed trait ApiVersion {
  val value: String
  val configPrefix: String
  override def toString: String = value
}
object VersionOne extends ApiVersion{
  override val value: String = "1.0"
  override val configPrefix: String = ""
}
object VersionTwo extends ApiVersion{
  override val value: String = "2.0"
  override val configPrefix: String = "v2."
}
object VersionThree extends ApiVersion{
  override val value: String = "3.0"
  override val configPrefix: String = "v3."
}

sealed trait AuthorisedAs
case class Csp(badgeIdentifier: BadgeIdentifier) extends AuthorisedAs
case class NonCsp(eori: Eori) extends AuthorisedAs

case class UpscanInitiatePayload(callbackUrl: String)

object UpscanInitiatePayload {
  implicit val format: OFormat[UpscanInitiatePayload] = Json.format[UpscanInitiatePayload]
}

case class AuthorisedRetrievalData(retrievalJSONBody: String)

case class FileUploadPayload(declarationID: String, documentationType: String)

case class UpscanInitiateResponsePayload(reference: String, uploadRequest: UpscanInitiateUploadRequest)

object UpscanInitiateUploadRequest {
  implicit val format: OFormat[UpscanInitiateUploadRequest] = Json.format[UpscanInitiateUploadRequest]
}

case class UpscanInitiateUploadRequest
(
  href: String,
  fields: Map[String, String]
)
{
  def addChild(n: NodeSeq, newChild: NodeSeq): NodeSeq = n match {
    case Elem(prefix, label, attribs, scope, child @ _*) =>
      Elem(prefix, label, attribs, scope, true, child ++ newChild : _*)
    case _ => sys.error("Can only add children to elements!")
  }

  def toXml: NodeSeq = {
    var xmlFields: NodeSeq = <fields></fields>

    fields.foreach { f =>
      val tag = f._1
      val content = f._2
      xmlFields = addChild(xmlFields, <a/>.copy(label = tag, child = scala.xml.Text(content)))
    }
    <fileUpload>
      <href>
        {href}
      </href>
      {xmlFields}
    </fileUpload>
  }
}

object UpscanInitiateResponsePayload {
  implicit val format: OFormat[UpscanInitiateResponsePayload] = Json.format[UpscanInitiateResponsePayload]
}

case class GoogleAnalyticsRequest(payload: String)

object GoogleAnalyticsRequest {
  implicit val format = Json.format[GoogleAnalyticsRequest]
}

case class NrsMetadata(businessId: String, notableEvent: String, payloadContentType: String, payloadSha256Checksum: String,
                       userSubmissionTimestamp: String, identityData: NrsRetrievalData, headerData: JsValue, searchKeys: JsValue)

object NrsMetadata {
  implicit val format = Json.format[NrsMetadata]
}

case class NrsPayload(payload: String, metadata: NrsMetadata)

object NrsPayload {
  implicit val format = Json.format[NrsPayload]
}

case class NrsResponsePayload(nrSubmissionId: NrSubmissionId)

object NrsResponsePayload {
  implicit val format = Json.format[NrsResponsePayload]
}
