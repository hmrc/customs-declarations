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

package uk.gov.hmrc.customs.declaration.model

import java.util.UUID

import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole}

case class RequestedVersion(versionNumber: String, configPrefix: Option[String])

case class Eori(value: String) extends AnyVal {
  override def toString: String = value.toString
}
object Eori {
  implicit val writer: Writes[Eori] = Writes[Eori] { x => JsString(x.value) }
  implicit val reader: Reads[Eori] = Reads.of[String].map(new Eori(_))
}

case class BadgeIdentifierEoriPair(badgeIdentifier: BadgeIdentifier, eori: Eori)

case class NrSubmissionId(nrSubmissionId: UUID) extends AnyVal {
  override def toString: String = nrSubmissionId.toString
}

object NrSubmissionId {
  implicit val format: OFormat[NrSubmissionId] = Json.format[NrSubmissionId]
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
  implicit val credentialsFormat: OFormat[Credentials] = Json.format[Credentials]
  implicit val nameFormat: OFormat[Name] = Json.format[Name]
  implicit val agentInformationFormat: OFormat[AgentInformation] = Json.format[AgentInformation]
  implicit val mdtpInformationFormat: OFormat[MdtpInformation] = Json.format[MdtpInformation]
  implicit val itmpNameFormat: OFormat[ItmpName] = Json.format[ItmpName]
  implicit val itmpAddressFormat: OFormat[ItmpAddress] = Json.format[ItmpAddress]
  implicit val loginTimesFormat: OFormat[LoginTimes] = Json.format[LoginTimes]
  implicit val nrsRetrievalData: OFormat[NrsRetrievalData] = Json.format[NrsRetrievalData]
}

case class ClientId(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class ConversationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}
object ConversationId {
  implicit val writer: Writes[ConversationId] = Writes[ConversationId] { x => JsString(x.uuid.toString) }
  implicit val reader: Reads[ConversationId] = Reads.of[UUID].map(new ConversationId(_))
}

case class Mrn(value: String) extends AnyVal {
  override def toString: String = value.toString
}

sealed trait GoogleAnalyticsValues {
  val enabled: Boolean = true
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

  val FileUpload: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override val success: String = "declarationFileUploadSuccess"
    override val failure: String = "declarationFileUploadFailure"
  }

  val Clearance: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override val success: String = "declarationClearanceSuccess"
    override val failure: String = "declarationClearanceFailure"
  }

  val Amend: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override lazy val success: String = "declarationAmendSuccess"
    override lazy val failure: String = "declarationAmendFailure"
  }

  val ArrivalNotification: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override val success: String = "declarationArrivalNotificationSuccess"
    override val failure: String = "declarationArrivalNotificationFailure"
  }

  val DeclarationStatus: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override val success: String = "declarationStatusSuccess"
    override val failure: String = "declarationStatusFailure"
  }
}


case class CorrelationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

case class DeclarationManagementInformationRequestId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

case class BadgeIdentifier(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class SubscriptionFieldsId(value: UUID) extends AnyVal{
  override def toString: String = value.toString
}
object SubscriptionFieldsId {
  implicit val writer: Writes[SubscriptionFieldsId] = Writes[SubscriptionFieldsId] { x => JsString(x.value.toString) }
  implicit val reader: Reads[SubscriptionFieldsId] = Reads.of[UUID].map(new SubscriptionFieldsId(_))
}

case class DeclarationId(value: String) extends AnyVal{
  override def toString: String = value.toString
}
object DeclarationId {
  implicit val writer: Writes[DeclarationId] = Writes[DeclarationId] { x => JsString(x.value) }
  implicit val reader: Reads[DeclarationId] = Reads.of[String].map(new DeclarationId(_))
}

case class DocumentationType(value: String) extends AnyVal{
  override def toString: String = value.toString
}

object DocumentationType {
  implicit val writer: Writes[DocumentationType] = Writes[DocumentationType] { x => JsString(x.value) }
  implicit val reader: Reads[DocumentationType] = Reads.of[String].map(new DocumentationType(_))
}

case class FileSequenceNo(value: Int) extends AnyVal{
  override def toString: String = value.toString
}
object FileSequenceNo {
  implicit val writer: Writes[FileSequenceNo] = Writes[FileSequenceNo] { x =>
    val d: BigDecimal = x.value
    JsNumber(d)
  }
  implicit val reader: Reads[FileSequenceNo] = Reads.of[Int].map(new FileSequenceNo(_))
}

case class FileGroupSize(value: Int) extends AnyVal{
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

sealed trait AuthorisedAs {
  val retrievalData: Option[NrsRetrievalData]
}
sealed trait AuthorisedAsCsp extends AuthorisedAs {
  val badgeIdentifier: BadgeIdentifier
  val retrievalData: Option[NrsRetrievalData]
}
case class Csp(badgeIdentifier: BadgeIdentifier, retrievalData: Option[NrsRetrievalData]) extends AuthorisedAsCsp
case class CspWithEori(badgeIdentifier: BadgeIdentifier, eori: Eori, retrievalData: Option[NrsRetrievalData]) extends AuthorisedAsCsp
case class NonCsp(eori: Eori, retrievalData: Option[NrsRetrievalData]) extends AuthorisedAs

case class UpscanInitiatePayload(callbackUrl: String)

object UpscanInitiatePayload {
  implicit val format: OFormat[UpscanInitiatePayload] = Json.format[UpscanInitiatePayload]
}

case class AuthorisedRetrievalData(retrievalJSONBody: String)

case class UpscanInitiateResponsePayload(reference: String, uploadRequest: UpscanInitiateUploadRequest)

object UpscanInitiateUploadRequest {
  implicit val format: OFormat[UpscanInitiateUploadRequest] = Json.format[UpscanInitiateUploadRequest]
}

case class UpscanInitiateUploadRequest
(
  href: String,
  fields: Map[String, String]
)

object UpscanInitiateResponsePayload {
  implicit val format: OFormat[UpscanInitiateResponsePayload] = Json.format[UpscanInitiateResponsePayload]
}

case class GoogleAnalyticsRequest(payload: String)

object GoogleAnalyticsRequest {
  implicit val format: OFormat[GoogleAnalyticsRequest] = Json.format[GoogleAnalyticsRequest]
}

case class NrsMetadata(businessId: String, notableEvent: String, payloadContentType: String, payloadSha256Checksum: String,
                       userSubmissionTimestamp: String, identityData: NrsRetrievalData, userAuthToken: String, headerData: JsValue,
                       searchKeys: JsValue, nrsSubmissionId: String)

object NrsMetadata {
  implicit val format: OFormat[NrsMetadata] = Json.format[NrsMetadata]
}

case class NrsPayload(payload: String, metadata: NrsMetadata)

object NrsPayload {
  implicit val format: OFormat[NrsPayload] = Json.format[NrsPayload]
}

private object NotAvailable { val na = Some("NOT AVAILABLE") }

case class DeclarationManagementInformationResponse(declaration: Declaration)
case class Declaration(versionNumber: Option[String] = NotAvailable.na, creationDate: Option[String] = NotAvailable.na, acceptanceDate: Option[String] = NotAvailable.na,  tradeMovementType: Option[String] = NotAvailable.na,  `type`: Option[String] = NotAvailable.na,  parties: Parties, goodsItemCount: Option[String] = NotAvailable.na,  packageCount: Option[String] = NotAvailable.na)
case class Parties(partyIdentification: PartyIdentification)
case class PartyIdentification(number: Option[String] = NotAvailable.na)

object PartyIdentification { implicit val format: OFormat[PartyIdentification] = Json.format[PartyIdentification] }
object Parties { implicit val format: OFormat[Parties] = Json.format[Parties] }
object Declaration { implicit val format: OFormat[Declaration] = Json.format[Declaration] }
object DeclarationManagementInformationResponse { implicit val format: OFormat[DeclarationManagementInformationResponse] = Json.format[DeclarationManagementInformationResponse] }
