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

package uk.gov.hmrc.customs.declaration.model

import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole}

import java.time.LocalDate
import java.util.UUID

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
  implicit val format: Format[NrSubmissionId] = new Format[NrSubmissionId]:
    override def writes(o: NrSubmissionId): JsValue = JsString(o.nrSubmissionId.toString)

    override def reads(json: JsValue): JsResult[NrSubmissionId] = json.validate[String].map { str =>
      NrSubmissionId(UUID.fromString(str))
    }

}

case class NrsRetrievalData(internalId: Option[String],
  externalId: Option[String],
  agentCode: Option[String],
  credentials: Option[Credentials],
  confidenceLevel: ConfidenceLevel,
  nino: Option[String],
  saUtr: Option[String],
  name: Option[Name],
  dateOfBirth: Option[LocalDate],
  email: Option[String],
  agentInformation: AgentInformation,
  groupIdentifier: Option[String],
  credentialRole: Option[CredentialRole],
  mdtpInformation: Option[MdtpInformation],
  itmpName: Option[ItmpName],
  itmpDateOfBirth: Option[LocalDate],
  itmpAddress: Option[ItmpAddress],
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

case class CorrelationId(uuid: UUID) extends AnyVal {
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
  val eori: Option[Eori]
  val badgeIdentifier: Option[BadgeIdentifier]
  val retrievalData: Option[NrsRetrievalData]
}
case class Csp(eori: Option[Eori], badgeIdentifier: Option[BadgeIdentifier], retrievalData: Option[NrsRetrievalData]) extends AuthorisedAsCsp
object Csp {
  def originatingPartyId(csp: Csp): String = csp.eori.fold(csp.badgeIdentifier.get.toString)(e => e.toString)
}
case class NonCsp(eori: Eori, retrievalData: Option[NrsRetrievalData]) extends AuthorisedAs

case class UpscanInitiatePayload(callbackUrl: String, maximumFileSize: Int, successRedirect: Option[String], errorRedirect: Option[String]){
  val isV2: Boolean = successRedirect.isDefined && errorRedirect.isDefined
}

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

case class Declaration(versionNumber: Option[String] = NotAvailable.na, creationDate: Option[String] = NotAvailable.na, acceptanceDate: Option[String] = NotAvailable.na,  tradeMovementType: Option[String] = NotAvailable.na,  `type`: Option[String] = NotAvailable.na,  parties: Parties, goodsItemCount: Option[String] = NotAvailable.na,  packageCount: Option[String] = NotAvailable.na)
case class Parties(partyIdentification: PartyIdentification)
case class PartyIdentification(number: Option[String] = NotAvailable.na)

object PartyIdentification { implicit val format: OFormat[PartyIdentification] = Json.format[PartyIdentification] }
object Parties { implicit val format: OFormat[Parties] = Json.format[Parties] }
object Declaration { implicit val format: OFormat[Declaration] = Json.format[Declaration] }
