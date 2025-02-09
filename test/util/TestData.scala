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

package util

import com.google.inject.AbstractModule
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.{AnyContentAsJson, AnyContentAsXml}
import play.api.test.FakeRequest
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.ConfidenceLevel.L500
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.model.upscan._
import uk.gov.hmrc.customs.declaration.services.{UniqueIdsService, UuidService}
import unit.logging.StubDeclarationsLogger
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.RequestHeaders.{ValidHeadersV1, ValidHeadersV2, ValidHeadersV3, X_EORI_IDENTIFIER_NAME}
import util.TestData.declarantEori

import java.net.URL
import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}
import java.util.UUID
import java.util.UUID.fromString
import scala.xml.{Elem, NodeSeq}

object TestData {
  val conversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val conversationIdUuid: UUID = fromString(conversationIdValue)
  val conversationId: ConversationId = ConversationId(conversationIdUuid)

  val correlationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val correlationIdUuid: UUID = fromString(correlationIdValue)
  val correlationId: CorrelationId = CorrelationId(correlationIdUuid)

  val mrnValue = "theMrn"
  val mrn: Mrn = Mrn(mrnValue)

  val dateString = "2018-09-11T10:28:54.128Z"
  val date: Instant = Instant.parse("2018-09-11T10:28:54.128Z")
  val httpFormattedDate = "Tue, 11 Sep 2018 10:28:54 UTC"

  val subscriptionFieldsIdString: String = "b82f31c6-2239-4253-b6f5-ed75e37ab7a5"
  val subscriptionFieldsIdUuid: UUID = fromString("b82f31c6-2239-4253-b6f5-ed75e37ab7a5")

  val clientSubscriptionIdString: String = "327d9145-4965-4d28-a2c5-39dedee50334"

  val nrSubmissionId: NrSubmissionId = NrSubmissionId(conversationId.uuid)
  val nrsConfigEnabled: NrsConfig = NrsConfig(nrsEnabled = true, "nrs-api-key", "nrs.url")
  val nrsConfigDisabled: NrsConfig = NrsConfig(nrsEnabled = false, "nrs-api-key",  "nrs.url")

  val allVersionsUnshuttered: DeclarationsShutterConfig = DeclarationsShutterConfig(Some(false), Some(false), Some(false))

  val TenMb = 10485760
  val fileUploadConfig: FileUploadConfig = FileUploadConfig("upscan-initiate-v1.url", "upscan-initiate-v2.url", 10485760, "callback.url", 3, "fileTransmissionCallbackUrl", "fileTransmissionUrl", 600)

  val validBadgeIdentifierValue = "BADGEID123"
  val invalidBadgeIdentifierValue = "INVALIDBADGEID123456789"
  val invalidBadgeIdentifier: BadgeIdentifier = BadgeIdentifier(invalidBadgeIdentifierValue)
  val badgeIdentifier: BadgeIdentifier = BadgeIdentifier(validBadgeIdentifierValue)

  val cspBearerToken = "CSP-Bearer-Token"
  val nonCspBearerToken = "Software-House-Bearer-Token"
  val invalidBearerToken = "InvalidBearerToken"

  val declarantEoriValue = "ZZ123456789000"
  val declarantEori: Eori = Eori(declarantEoriValue)
  val invalidDeclarantEoriValue = "ZZ123456789000123456789"
  val invalidDeclarantEori: Eori = Eori(invalidDeclarantEoriValue)
  val upscanInitiateReference = "11370e18-6e24-453e-b45a-76d3e32ea33d"
  val ONE = 1
  val TWO = 2
  val THREE = 3
  val FOUR = 4

  val ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = ValidatedFileUploadPayloadRequest(
    ConversationId(UUID.randomUUID()),
    EventStart,
    VersionTwo,
    ClientId("ABC"),
    NonCsp(Eori("123"), None),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request")),
    FileUploadRequest(DeclarationId("decId123"),FileGroupSize(TWO),
    Seq(FileUploadFile(FileSequenceNo(ONE), maybeDocumentType = None, Some("https://success-redirect.com"), Some("https://error-redirect.com")), FileUploadFile(FileSequenceNo(TWO), Some(DocumentType("docType2")), Some("https://success-redirect.com"), Some("https://error-redirect.com"))))
  )

  val ValidatedFileUploadPayloadRequestForCspWithTwoFiles: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = ValidatedFileUploadPayloadRequest(
    ConversationId(UUID.randomUUID()),
    EventStart,
    VersionTwo,
    ClientId("ABC"),
    Csp(Some(Eori("123")), Some(badgeIdentifier), None),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request")),
    FileUploadRequest(DeclarationId("decId123"),FileGroupSize(TWO),
    Seq(FileUploadFile(FileSequenceNo(ONE), Some(DocumentType("docType1")), Some("https://success-redirect.com"), Some("https://error-redirect.com")), FileUploadFile(FileSequenceNo(TWO), Some(DocumentType("docType2")), Some("https://success-redirect.com"), Some("https://error-redirect.com"))))
  )

  val ValidatedFileUploadPayloadRequestWithFourFiles: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = ValidatedFileUploadPayloadRequest(
    ConversationId(UUID.randomUUID()),
    EventStart,
    VersionTwo,
    ClientId("ABC"),
    NonCsp(Eori("123"), None),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request")),
    FileUploadRequest(
      DeclarationId("decId123"),
      FileGroupSize(FOUR),
      Seq(FileUploadFile(FileSequenceNo(ONE), maybeDocumentType = None, Some("https://success-redirect.com"), Some("https://error-redirect.com")),
        FileUploadFile(FileSequenceNo(TWO), Some(DocumentType("docType2")), Some("https://success-redirect.com"), Some("https://error-redirect.com")),
        FileUploadFile(FileSequenceNo(THREE), Some(DocumentType("docType3")), Some("https://success-redirect.com"), Some("https://error-redirect.com")),
        FileUploadFile(FileSequenceNo(FOUR), Some(DocumentType("docType4")), Some("https://success-redirect.com"), Some("https://error-redirect.com"))))
  )

  val nrsInternalIdValue: String = "internalId"
  val nrsExternalIdValue: String = "externalId"
  val nrsAgentCodeValue: String = "agentCode"
  val nrsCredentials: Option[Credentials] = Some(Credentials(providerId= "providerId", providerType= "providerType"))
  val nrsConfidenceLevel: ConfidenceLevel.L500.type = L500
  val nrsNinoValue: String = "ninov"
  val nrsSaUtrValue: String = "saUtr"
  val nrsNameValue: Option[Name] = Some(Name(Some("name"), Some("lastname")))
  val TWENTY_FIVE = 25
  val nrsDateOfBirth: Option[LocalDate] = Some(LocalDate.now().minusYears(TWENTY_FIVE))
  val nrsEmailValue: Option[String] = Some("nrsEmailValue")
  val nrsAgentInformationValue: AgentInformation = AgentInformation(Some("agentId"),
                                                  Some("agentCode"),
                                                  Some("agentFriendlyName"))
  val nrsGroupIdentifierValue: Some[String] = Some("groupIdentifierValue")
  val nrsCredentialRole: Some[User.type] = Some(User)
  val nrsMdtpInformation: MdtpInformation = MdtpInformation("deviceId", "sessionId")
  val nrsItmpName: Some[ItmpName] = Some(ItmpName(Some("givenName"),
                            Some("middleName"),
                            Some("familyName")))
  val nrsItmpAddress: Some[ItmpAddress] = Some(ItmpAddress(Some("line1"),
                                  Some("line2"),
                                  Some("line3"),
                                  Some("line4"),
                                  Some("line5"),
                                  Some("postCode"),
                                  Some("countryName"),
                                  Some("countryCode")))
  val nrsAffinityGroup: Some[AffinityGroup.Individual.type] = Some(Individual)
  val nrsCredentialStrength: Some[String] = Some("STRONG")

  val CURRENT_TIME_IN_MILLIS = 1530442800000L
  val PREVIOUS_TIME_IN_MILLIS = 1530464400000L
  val NRS_TIMESTAMP_IN_MILLIS = 1530475200000L

  val currentLoginTime: ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(CURRENT_TIME_IN_MILLIS),  ZoneId.of("UTC"))
  val currentLoginTime_2: ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(CURRENT_TIME_IN_MILLIS), ZoneId.of("UTC"))
  val previousLoginTime: ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(PREVIOUS_TIME_IN_MILLIS), ZoneId.of("UTC"))
  val previousLoginTime_2: ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(PREVIOUS_TIME_IN_MILLIS), ZoneId.of("UTC"))
  val nrsTimeStamp: Instant = Instant.ofEpochMilli(NRS_TIMESTAMP_IN_MILLIS)

  val nrsLoginTimes: LoginTimes = LoginTimes(currentLoginTime.toInstant, Some(previousLoginTime.toInstant))

  val nrsRetrievalValues: NrsRetrievalData = NrsRetrievalData(Some(nrsInternalIdValue),
    Some(nrsExternalIdValue),
    Some(nrsAgentCodeValue),
    nrsCredentials,
    nrsConfidenceLevel,
    Some(nrsNinoValue),
    Some(nrsSaUtrValue),
    nrsNameValue,
    nrsDateOfBirth,
    nrsEmailValue,
    nrsAgentInformationValue,
    nrsGroupIdentifierValue,
    nrsCredentialRole,
    Some(nrsMdtpInformation),
    nrsItmpName,
    nrsDateOfBirth,
    nrsItmpAddress,
    nrsAffinityGroup,
    nrsCredentialStrength,
    nrsLoginTimes)

  val nrsRetrievalData: Retrieval[Option[String] ~ Option[String] ~ Option[String] ~ Option[Credentials] ~ ConfidenceLevel ~ Option[String] ~ Option[String] ~ Option[Name] ~ Option[LocalDate] ~ Option[String] ~ AgentInformation ~ Option[String] ~ Option[CredentialRole] ~ Option[MdtpInformation] ~ Option[ItmpName] ~ Option[LocalDate] ~ Option[ItmpAddress] ~ Option[AffinityGroup] ~ Option[String] ~ LoginTimes] = Retrievals.internalId and Retrievals.externalId and Retrievals.agentCode and Retrievals.credentials and Retrievals.confidenceLevel and
    Retrievals.nino and Retrievals.saUtr and Retrievals.name and Retrievals.dateOfBirth and
    Retrievals.email and Retrievals.agentInformation and Retrievals.groupIdentifier and Retrievals.credentialRole and Retrievals.mdtpInformation and
    Retrievals.itmpName and Retrievals.itmpDateOfBirth and Retrievals.itmpAddress and Retrievals.affinityGroup and Retrievals.credentialStrength and Retrievals.loginTimes

  val nrsReturnData = new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~(new ~( new ~(new ~(Some(nrsInternalIdValue)
    ,Some(nrsExternalIdValue)),
    Some(nrsAgentCodeValue)),
    nrsCredentials),
    nrsConfidenceLevel),
    Some(nrsNinoValue)),
    Some(nrsSaUtrValue)),
    nrsNameValue),
    nrsDateOfBirth),
    nrsEmailValue),
    nrsAgentInformationValue),
    nrsGroupIdentifierValue),
    nrsCredentialRole),
    Some(nrsMdtpInformation)),
    nrsItmpName),
    nrsDateOfBirth),
    nrsItmpAddress),
    nrsAffinityGroup),
    nrsCredentialStrength),
    nrsLoginTimes)

  val cspNrsMetadata = new NrsMetadata("cds", "cds-declaration", "application/xml",
    "9aa7c53a734c517fa70edf946f113b123b1d43556ca558235826e145df70051d", nrsTimeStamp.toString,
    nrsRetrievalValues, "bearer-token", Json.parse("""{"Host":"localhost","Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))), conversationIdValue)


  val nrsMetadata = new NrsMetadata("cds", "cds-declaration", "application/xml",
    "9aa7c53a734c517fa70edf946f113b123b1d43556ca558235826e145df70051d", nrsTimeStamp.toString,
    nrsRetrievalValues, "bearer-token", Json.parse("""{"Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))), conversationIdValue)

  val cspNrsMetadataMultipleHeaderValues = new NrsMetadata("cds", "cds-declaration", "application/xml",
    "9aa7c53a734c517fa70edf946f113b123b1d43556ca558235826e145df70051d", nrsTimeStamp.toString,
    nrsRetrievalValues, "bearer-token", Json.parse("""{"Host":"localhost","Accept":"ABC,DEF","Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))), conversationIdValue)

  val nrsPayload = new NrsPayload("PGZvbz5iYXI8L2Zvbz4=", nrsMetadata)
  val cspNrsPayload = new NrsPayload("PGZvbz5iYXI8L2Zvbz4=", cspNrsMetadata)

  val cspNrsPayloadMultipleHeaderValues = new NrsPayload("PGZvbz5iYXI8L2Zvbz4=", cspNrsMetadataMultipleHeaderValues)

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  type EmulatedInsufficientEnrolments = InsufficientEnrolments
  val emulatedInsufficientEnrolments = new EmulatedInsufficientEnrolments("Insufficient Enrolments")
  
  lazy val mockUuidService: UuidService = mock[UuidService]

  lazy val stubDeclarationsLogger = new StubDeclarationsLogger(mock[CdsLogger])

  object TestModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[UuidService]) `toInstance` mockUuidService
    }

    def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
  }

  // note we can not mock service methods that return value classes - however using a simple stub IMHO it results in cleaner code (less mocking noise)
  lazy val stubUniqueIdsService: UniqueIdsService = new UniqueIdsService(mockUuidService) {
    override def conversation: ConversationId = conversationId

    override def correlation: CorrelationId = correlationId

  }

  val TestXmlPayload: Elem = <foo>bar</foo>
  val TestFakeRequest: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(("Authorization", "bearer-token"))
  val TestFakeRequestWithBadgeIdAndNoEori: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(("Authorization", "bearer-token"), ("X-Badge-Identifier", badgeIdentifier.value))
  val TestFakeRequestWithEoriAndNoBadgeId: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(("Authorization", "bearer-token"), ("X-EORI-Identifier", declarantEori.value))
  val TestFakeRequestMultipleHeaderValues: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(("Authorization", "bearer-token"), ("Accept", "ABC"), ("Accept", "DEF"))
  val TestFakeRequestWithV1Headers: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(ValidHeadersV1.toSeq*)
  val TestFakeRequestWithV2Headers: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(ValidHeadersV2.toSeq*)
  val TestFakeRequestWithV3Headers: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(ValidHeadersV3.toSeq*)

  def testFakeRequestWithBadgeId(badgeIdString: String = badgeIdentifier.value): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(RequestHeaders.X_BADGE_IDENTIFIER_NAME -> badgeIdString)

  def testFakeRequestWithBadgeIdEoriPair(badgeIdString: String = badgeIdentifier.value, eoriString: String = declarantEori.value, eoriHeaderName: String = X_EORI_IDENTIFIER_NAME): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(RequestHeaders.X_BADGE_IDENTIFIER_NAME -> badgeIdString, eoriHeaderName -> eoriString)

  def testFakeRequestWithEoriNoBadgeId(eoriString: String = declarantEori.value, eoriHeaderName: String = X_EORI_IDENTIFIER_NAME): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(eoriHeaderName -> eoriString)

  def testFakeRequestWithInvalidEoriNoBadgeId(eoriHeaderName: String = X_EORI_IDENTIFIER_NAME): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(eoriHeaderName -> invalidDeclarantEoriValue)

  def testFakeRequestWithInvalidBadgeIdNoEori(): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(RequestHeaders.X_BADGE_IDENTIFIER_NAME -> invalidBadgeIdentifierValue)

  def testFakeRequestWithHeader(header: String, headerValue: String): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(header -> headerValue)

  val TestConversationIdRequest: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(conversationId, EventStart, TestFakeRequest)
  val TestConversationIdRequestWithV1Headers: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(conversationId, EventStart, TestFakeRequestWithV1Headers)
  val TestConversationIdRequestWithV2Headers: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(conversationId, EventStart, TestFakeRequestWithV2Headers)
  val TestConversationIdRequestWithV3Headers: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(conversationId, EventStart, TestFakeRequestWithV3Headers)
  val TestConversationIdRequestMultipleHeaderValues: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(conversationId, EventStart, TestFakeRequestMultipleHeaderValues)
  val TestConversationIdRequestWithBadgeIdAndNoEori: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(conversationId, EventStart, TestFakeRequestWithBadgeIdAndNoEori)
  val TestConversationIdRequestWithEoriAndNoBadgeId: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(conversationId, EventStart, TestFakeRequestWithEoriAndNoBadgeId)
  val TestApiVersionRequestV1: ApiVersionRequest[AnyContentAsXml] = TestConversationIdRequestWithV1Headers.toApiVersionRequest(VersionOne)
  val TestApiVersionRequestV2: ApiVersionRequest[AnyContentAsXml] = TestConversationIdRequestWithV2Headers.toApiVersionRequest(VersionTwo)
  val TestApiVersionRequestV3: ApiVersionRequest[AnyContentAsXml] = TestConversationIdRequestWithV3Headers.toApiVersionRequest(VersionThree)
  val TestApiVersionRequestMultipleHeaderValues: ApiVersionRequest[AnyContentAsXml] = TestConversationIdRequestMultipleHeaderValues.toApiVersionRequest(VersionOne)
  val TestApiVersionRequestWithBadgeIdAndNoEori: ApiVersionRequest[AnyContentAsXml] = TestConversationIdRequestWithBadgeIdAndNoEori.toApiVersionRequest(VersionOne)
  val TestApiVersionRequestWithEoriAndNoBadgeId: ApiVersionRequest[AnyContentAsXml] = TestConversationIdRequestWithEoriAndNoBadgeId.toApiVersionRequest(VersionOne)
  
  // For Status endpoint
  val TestConversationIdStatusRequest: ConversationIdRequest[AnyContentAsXml] = ConversationIdRequest(conversationId, EventStart, TestFakeRequest)
  val TestExtractedStatusHeaders: ExtractedStatusHeadersImpl = ExtractedStatusHeadersImpl(badgeIdentifier, ApiSubscriptionFieldsTestData.clientId)
  val TestValidatedHeadersStatusRequest: ValidatedHeadersStatusRequest[AnyContentAsXml] = TestApiVersionRequestV1.toValidatedHeadersStatusRequest(TestExtractedStatusHeaders)
  val TestAuthorisedStatusRequest: AuthorisedRequest[AnyContentAsXml] = TestValidatedHeadersStatusRequest.toAuthorisedRequest(Csp(None, Some(badgeIdentifier), None))

  val TestExtractedHeaders: ExtractedHeadersImpl = ExtractedHeadersImpl(ApiSubscriptionFieldsTestData.clientId)
  val TestValidatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsXml] = TestApiVersionRequestV1.toValidatedHeadersRequest(TestExtractedHeaders)

  val TestValidatedHeadersRequestMultipleHeaderValues: ValidatedHeadersRequest[AnyContentAsXml] = TestApiVersionRequestMultipleHeaderValues.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestCspAuthorisedRequest: AuthorisedRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), Some(nrsRetrievalValues)))
  val TestValidatedHeadersRequestNoBadge: ValidatedHeadersRequest[AnyContentAsXml] = TestApiVersionRequestV1.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestValidatedHeadersRequestWithBadgeIdAndNoEori: ValidatedHeadersRequest[AnyContentAsXml] = TestApiVersionRequestWithBadgeIdAndNoEori.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestValidatedHeadersRequestWithEoriAndNoBadgeId: ValidatedHeadersRequest[AnyContentAsXml] = TestApiVersionRequestWithEoriAndNoBadgeId.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestCspWithBadgeIdNoEoriValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier), Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestCspWithEoriNoBadgeIdValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(Csp(Some(declarantEori), None, Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestCspValidatedPayloadRequestMultipleHeaderValues: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequestMultipleHeaderValues.toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestNonCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toNonCspAuthorisedRequest(declarantEori, Some(nrsRetrievalValues)).toValidatedPayloadRequest(xmlBody = TestXmlPayload)

  val BatchIdOne: BatchId = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef001"))
  val BatchIdTwo: BatchId = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef002"))
  val BatchIdThree: BatchId = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef003"))
  val FileReferenceOne: FileReference = FileReference(fromString("31400000-8ce0-11bd-b23e-10b96e4ef00f"))
  val FileReferenceTwo: FileReference = FileReference(fromString("32400000-8cf0-11bd-b23e-10b96e4ef00f"))
  val FileReferenceThree: FileReference = FileReference(fromString("33400000-8cd0-11bd-b23e-10b96e4ef00f"))
  val InitiateDateAsString = "2018-04-24T09:30:00Z"
  val InitiateDate: Instant = Instant.parse(InitiateDateAsString)
  val createdAtDate: Instant = Instant.ofEpochMilli(InitiateDate.toEpochMilli)
  val CallbackFieldsOne: CallbackFields = CallbackFields("name1", "application/xml", "checksum1", InitiateDate, new URL("https://outbound.a.com"))
  val CallbackFieldsTwo: CallbackFields = CallbackFields("name2", "application/xml", "checksum2", InitiateDate, new URL("https://outbound.a.com"))
  val CallbackFieldsThree: CallbackFields = CallbackFields("name3", "application/xml", "checksum3", InitiateDate, new URL("https://outbound.a.com"))
  val CallbackFieldsUpdated: CallbackFields = CallbackFields("UPDATED_NAME", "UPDATED_MIMETYPE", "UPDATED_CHECKSUM", InitiateDate, new URL("https://outbound.a.com"))
  val BatchFileOne: BatchFile = BatchFile(reference = FileReferenceOne, Some(CallbackFieldsOne),
    inboundLocation = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(1), size = 1, documentType = Some(DocumentType("Document Type 1")))
  val BatchFileTwo: BatchFile = BatchFile(reference = FileReferenceTwo, Some(CallbackFieldsTwo),
    inboundLocation = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(2), size = 1, documentType = Some(DocumentType("Document Type 2")))
  val BatchFileThree: BatchFile = BatchFile(reference = FileReferenceThree, Some(CallbackFieldsThree),
    inboundLocation = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(3), size = 1, documentType = Some(DocumentType("Document Type 3")))
  val BatchFileOneNoCallbackFields: BatchFile = BatchFileOne.copy(maybeCallbackFields = None)
  val FileMetadataWithFileOne: FileUploadMetadata = FileUploadMetadata(DeclarationId("1"), Eori("123"), csId = subscriptionFieldsId, BatchIdOne, fileCount = 1, createdAt = createdAtDate, Seq(
    BatchFileOne
  ))
  val FileMetadataWithFileTwo: FileUploadMetadata = FileUploadMetadata(DeclarationId("2"), Eori("123"), csId = subscriptionFieldsId, BatchIdTwo, fileCount = 1, createdAt = createdAtDate, Seq(
    BatchFileTwo
  ))
  val FileMetadataWithFilesOneAndThree: FileUploadMetadata = FileUploadMetadata(DeclarationId("3"), Eori("123"), csId = subscriptionFieldsId, BatchIdThree, fileCount = 2, createdAt = createdAtDate, Seq(
    BatchFileOne, BatchFileThree
  ))
  val FileMetadataWithFileOneWithNoCallbackFieldsAndThree: FileUploadMetadata = FileUploadMetadata(DeclarationId("3"), Eori("123"), csId = subscriptionFieldsId, BatchIdOne, fileCount = 2, createdAt = createdAtDate, Seq(
    BatchFileOneNoCallbackFields, BatchFileThree
  ))

}

object RequestHeaders {

  val X_CONVERSATION_ID_NAME = "X-Conversation-ID"
  lazy val X_CONVERSATION_ID_HEADER: (String, String) = X_CONVERSATION_ID_NAME -> TestData.conversationId.toString

  val X_BADGE_IDENTIFIER_NAME = "X-Badge-Identifier"
  lazy val X_BADGE_IDENTIFIER_HEADER: (String, String) = X_BADGE_IDENTIFIER_NAME -> TestData.badgeIdentifier.value
  lazy val X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG: (String, String) = X_BADGE_IDENTIFIER_NAME -> TestData.invalidBadgeIdentifierValue
  val X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS: (String, String) = X_BADGE_IDENTIFIER_NAME -> "Invalid^&&("
  val X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_SHORT: (String, String) = X_BADGE_IDENTIFIER_NAME -> "12345"
  val X_BADGE_IDENTIFIER_HEADER_INVALID_LOWERCASE: (String, String) = X_BADGE_IDENTIFIER_NAME -> "BadgeId123"

  val X_CLIENT_ID_NAME = "X-Client-ID"
  val X_CLIENT_ID_HEADER: (String, String) = X_CLIENT_ID_NAME -> ApiSubscriptionFieldsTestData.xClientId
  val X_CLIENT_ID_HEADER_INVALID: (String, String) = X_CLIENT_ID_NAME -> "This is not a UUID"

  val X_EORI_IDENTIFIER_NAME = "X-EORI-Identifier"
  val X_EORI_IDENTIFIER_HEADER: (String, String) = X_EORI_IDENTIFIER_NAME -> declarantEori.value

  val X_SUBMITTER_IDENTIFIER_NAME = "X-Submitter-Identifier"
  val X_SUBMITTER_IDENTIFIER_HEADER: (String, String) = X_SUBMITTER_IDENTIFIER_NAME -> declarantEori.value

  val CONTENT_TYPE_HEADER: (String, String) = CONTENT_TYPE -> MimeTypes.XML
  val CONTENT_TYPE_CHARSET_VALUE: String = s"${MimeTypes.XML}; charset=UTF-8"
  val CONTENT_TYPE_CHARSET_INVALID_VALUE: String = s"${MimeTypes.XML}; charset=UTF-16"
  val CONTENT_TYPE_HEADER_CHARSET: (String, String) = CONTENT_TYPE -> CONTENT_TYPE_CHARSET_VALUE
  val CONTENT_TYPE_HEADER_INVALID: (String, String) = CONTENT_TYPE -> "somethinginvalid"

  val ACCEPT_HMRC_XML_V1_VALUE = "application/vnd.hmrc.1.0+xml"
  val ACCEPT_HMRC_XML_V2_VALUE = "application/vnd.hmrc.2.0+xml"
  val ACCEPT_HMRC_XML_V3_VALUE = "application/vnd.hmrc.3.0+xml"
  val ACCEPT_HMRC_XML_V1_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V1_VALUE
  val ACCEPT_HMRC_XML_V2_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V2_VALUE
  val ACCEPT_HMRC_XML_V3_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V3_VALUE
  val ACCEPT_HEADER_INVALID: (String, String) = ACCEPT -> "invalid"

  val ValidHeadersV1: Map[String, String] = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V1_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER,
    X_SUBMITTER_IDENTIFIER_HEADER
  )

  val ValidHeadersV2: Map[String, String] = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V2_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER,
    X_SUBMITTER_IDENTIFIER_HEADER
  )

  val ValidHeadersV2WithCharset: Map[String, String] = Map(
    CONTENT_TYPE -> CONTENT_TYPE_CHARSET_VALUE,
    ACCEPT_HMRC_XML_V2_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER,
    X_SUBMITTER_IDENTIFIER_HEADER
  )

  val ValidHeadersV2WithInvalidCharset: Map[String, String] = Map(
    CONTENT_TYPE -> CONTENT_TYPE_CHARSET_INVALID_VALUE,
    ACCEPT_HMRC_XML_V2_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER,
    X_SUBMITTER_IDENTIFIER_HEADER
  )

  val ValidHeadersV3: Map[String, String] = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V3_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER,
    X_SUBMITTER_IDENTIFIER_HEADER
  )
}
