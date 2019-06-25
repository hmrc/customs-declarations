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

package util

import java.net.URL
import java.time.Instant
import java.util.UUID
import java.util.UUID.fromString

import com.google.inject.AbstractModule
import org.joda.time.DateTimeZone.UTC
import org.joda.time.{DateTime, LocalDate}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import play.api.test.Helpers.CONTENT_TYPE
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.ConfidenceLevel.L500
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ValidatedFileUploadPayloadRequest, _}
import uk.gov.hmrc.customs.declaration.model.upscan._
import uk.gov.hmrc.customs.declaration.services.{UniqueIdsService, UuidService}
import unit.logging.StubDeclarationsLogger
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.TestData.declarantEori

import scala.xml.{Elem, NodeSeq}

object TestData {
  val conversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val conversationIdUuid: UUID = fromString(conversationIdValue)
  val conversationId: ConversationId = ConversationId(conversationIdUuid)

  val correlationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val correlationIdUuid: UUID = fromString(correlationIdValue)
  val correlationId = CorrelationId(correlationIdUuid)

  val dmirIdValue = "1b0a48a8-1259-42c9-9d6a-e797b919eb16"
  val dmirIdUuid: UUID = fromString(dmirIdValue)
  val dmirId = DeclarationManagementInformationRequestId(dmirIdUuid)

  val mrnValue = "theMrn"
  val mrn = Mrn(mrnValue)

  val dateString = "2018-09-11T10:28:54.128Z"
  val date: DateTime = DateTime.parse("2018-09-11T10:28:54.128Z")

  val subscriptionFieldsIdString: String = "b82f31c6-2239-4253-b6f5-ed75e37ab7a5"
  val subscriptionFieldsIdUuid: UUID = fromString("b82f31c6-2239-4253-b6f5-ed75e37ab7a5")

  val clientSubscriptionIdString: String = "327d9145-4965-4d28-a2c5-39dedee50334"

  val nrSubmissionId = NrSubmissionId(conversationId.uuid)
  val nrsConfigEnabled = NrsConfig(nrsEnabled = true, "nrs-api-key", "nrs.url")
  val nrsConfigDisabled = NrsConfig(nrsEnabled = false, "nrs-api-key",  "nrs.url")

  val TenMb = 10485760
  val fileUploadConfig = FileUploadConfig("upscan-initiate.url", "callback.url", 10485760, "callback.url", 3, "fileTransmissionCallbackUrl", "fileTransmissionUrl")

  val validBadgeIdentifierValue = "BADGEID123"
  val invalidBadgeIdentifierValue = "INVALIDBADGEID123456789"
  val invalidBadgeIdentifier: BadgeIdentifier = BadgeIdentifier(invalidBadgeIdentifierValue)
  val badgeIdentifier: BadgeIdentifier = BadgeIdentifier(validBadgeIdentifierValue)

  val cspBearerToken = "CSP-Bearer-Token"
  val nonCspBearerToken = "Software-House-Bearer-Token"
  val invalidBearerToken = "InvalidBearerToken"

  val declarantEoriValue = "ZZ123456789000"
  val declarantEori = Eori(declarantEoriValue)
  val upscanInitiateReference = "11370e18-6e24-453e-b45a-76d3e32ea33d"
  val ONE = 1
  val TWO = 2
  val THREE = 3
  val FOUR = 4

  val ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles = ValidatedFileUploadPayloadRequest(
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

  val ValidatedFileUploadPayloadRequestForCspWithTwoFiles = ValidatedFileUploadPayloadRequest(
    ConversationId(UUID.randomUUID()),
    EventStart,
    VersionTwo,
    ClientId("ABC"),
    CspWithEori(badgeIdentifier, Eori("123"), None),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request")),
    FileUploadRequest(DeclarationId("decId123"),FileGroupSize(TWO),
    Seq(FileUploadFile(FileSequenceNo(ONE), Some(DocumentType("docType1")), Some("https://success-redirect.com"), Some("https://error-redirect.com")), FileUploadFile(FileSequenceNo(TWO), Some(DocumentType("docType2")), Some("https://success-redirect.com"), Some("https://error-redirect.com"))))
  )

  val ValidatedFileUploadPayloadRequestWithFourFiles = ValidatedFileUploadPayloadRequest(
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
  val nrsCredentials: Credentials = Credentials(providerId= "providerId", providerType= "providerType")
  val nrsConfidenceLevel: ConfidenceLevel.L500.type = L500
  val nrsNinoValue: String = "ninov"
  val nrsSaUtrValue: String = "saUtr"
  val nrsNameValue: Name = Name(Some("name"), Some("lastname"))
  val TWENTY_FIVE = 25
  val nrsDateOfBirth: Option[LocalDate] = Some(LocalDate.now().minusYears(TWENTY_FIVE))
  val nrsEmailValue: Option[String] = Some("nrsEmailValue")
  val nrsAgentInformationValue: AgentInformation = AgentInformation(Some("agentId"),
                                                  Some("agentCode"),
                                                  Some("agentFriendlyName"))
  val nrsGroupIdentifierValue = Some("groupIdentifierValue")
  val nrsCredentialRole = Some(User)
  val nrsMdtpInformation = MdtpInformation("deviceId", "sessionId")
  val nrsItmpName = ItmpName(Some("givenName"),
                            Some("middleName"),
                            Some("familyName"))
  val nrsItmpAddress = ItmpAddress(Some("line1"),
                                  Some("line2"),
                                  Some("line3"),
                                  Some("line4"),
                                  Some("line5"),
                                  Some("postCode"),
                                  Some("countryName"),
                                  Some("countryCode"))
  val nrsAffinityGroup = Some(Individual)
  val nrsCredentialStrength = Some("STRONG")

  val CURRENT_TIME_IN_MILLIS = 1530442800000L
  val PREVIOUS_TIME_IN_MILLIS = 1530464400000L
  val NRS_TIMESTAMP_IN_MILLIS = 1530475200000L
  val currentLoginTime: DateTime = new DateTime(CURRENT_TIME_IN_MILLIS, UTC)
  val previousLoginTime: DateTime = new DateTime(PREVIOUS_TIME_IN_MILLIS, UTC)
  val nrsTimeStamp: DateTime = new DateTime(NRS_TIMESTAMP_IN_MILLIS, UTC)

  val nrsLoginTimes = LoginTimes(currentLoginTime, Some(previousLoginTime))

  val nrsRetrievalValues = NrsRetrievalData(Some(nrsInternalIdValue),
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

  val nrsRetrievalData: Retrieval[Option[String] ~ Option[String] ~ Option[String] ~ Credentials ~ ConfidenceLevel ~ Option[String] ~ Option[String] ~ Name ~ Option[LocalDate] ~ Option[String] ~ AgentInformation ~ Option[String] ~ Option[CredentialRole] ~ Option[MdtpInformation] ~ ItmpName ~ Option[LocalDate] ~ ItmpAddress ~ Option[AffinityGroup] ~ Option[String] ~ LoginTimes] = Retrievals.internalId and Retrievals.externalId and Retrievals.agentCode and Retrievals.credentials and Retrievals.confidenceLevel and
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
    "9aa7c53a734c517fa70edf946f113b123b1d43556ca558235826e145df70051d", nrsTimeStamp.toString, nrsRetrievalValues, "bearer-token", Json.parse("""{"Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))), conversationIdValue)


  val nrsMetadata = new NrsMetadata("cds", "cds-declaration", "application/xml",
    "9aa7c53a734c517fa70edf946f113b123b1d43556ca558235826e145df70051d", nrsTimeStamp.toString, nrsRetrievalValues, "bearer-token", Json.parse("""{"Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))), conversationIdValue)

  val cspNrsMetadataMultipleHeaderValues = new NrsMetadata("cds", "cds-declaration", "application/xml",
    "9aa7c53a734c517fa70edf946f113b123b1d43556ca558235826e145df70051d", nrsTimeStamp.toString, nrsRetrievalValues, "bearer-token", Json.parse("""{"Accept":"ABC,DEF","Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))), conversationIdValue)

  val nrsPayload = new NrsPayload("PGZvbz5iYXI8L2Zvbz4=", nrsMetadata)
  val cspNrsPayload = new NrsPayload("PGZvbz5iYXI8L2Zvbz4=", cspNrsMetadata) // <foo>bar</foo>

  val cspNrsPayloadMultipleHeaderValues = new NrsPayload("PGZvbz5iYXI8L2Zvbz4=", cspNrsMetadataMultipleHeaderValues)

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  lazy val mockUuidService: UuidService = mock[UuidService]

  lazy val stubDeclarationsLogger = new StubDeclarationsLogger(mock[CdsLogger])

  object TestModule extends AbstractModule {
    def configure(): Unit = {
      bind(classOf[UuidService]) toInstance mockUuidService
    }

    def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
  }

  // note we can not mock service methods that return value classes - however using a simple stub IMHO it results in cleaner code (less mocking noise)
  lazy val stubUniqueIdsService: UniqueIdsService = new UniqueIdsService(mockUuidService) {
    override def conversation: ConversationId = conversationId

    override def correlation: CorrelationId = correlationId

    override def dmir: DeclarationManagementInformationRequestId = dmirId
  }

  val TestXmlPayload: Elem = <foo>bar</foo>
  val TestFakeRequest: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(("Authorization", "bearer-token"))
  val TestFakeRequestWithBadgeIdAndNoEori: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(("Authorization", "bearer-token"), ("X-Badge-Identifier", badgeIdentifier.value))
  val TestFakeRequestWithEoriAndNoBadgeId: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(("Authorization", "bearer-token"), ("X-EORI-Identifier", declarantEori.value))
  val TestFakeRequestMultipleHeaderValues: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload).withHeaders(("Authorization", "bearer-token"), ("Accept", "ABC"), ("Accept", "DEF"))

  def testFakeRequestWithBadgeId(badgeIdString: String = badgeIdentifier.value): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(RequestHeaders.X_BADGE_IDENTIFIER_NAME -> badgeIdString)

  def testFakeRequestWithBadgeIdEoriPair(badgeIdString: String = badgeIdentifier.value, eoriString: String = declarantEori.value): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(RequestHeaders.X_BADGE_IDENTIFIER_NAME -> badgeIdString, RequestHeaders.X_EORI_IDENTIFIER_NAME -> eoriString)

  def testFakeRequestWithHeader(header: String, headerValue: String): FakeRequest[AnyContentAsXml] =
    FakeRequest().withXmlBody(TestXmlPayload).withHeaders(header -> headerValue)

  // For Status endpoint
  val TestConversationIdStatusRequest = ConversationIdRequest(conversationId, EventStart, TestFakeRequest)
  val TestExtractedStatusHeaders = ExtractedStatusHeadersImpl(VersionTwo, badgeIdentifier, ApiSubscriptionFieldsTestData.clientId)
  val TestValidatedHeadersStatusRequest: ValidatedHeadersStatusRequest[AnyContentAsXml] = TestConversationIdStatusRequest.toValidatedHeadersStatusRequest(TestExtractedStatusHeaders)
  val TestAuthorisedStatusRequest: AuthorisedStatusRequest[AnyContentAsXml] = TestValidatedHeadersStatusRequest.toAuthorisedStatusRequest

  val TestConversationIdRequest = ConversationIdRequest(conversationId, EventStart, TestFakeRequest)
  val TestConversationIdRequestWithBadgeIdAndNoEori = ConversationIdRequest(conversationId, EventStart, TestFakeRequestWithBadgeIdAndNoEori)
  val TestConversationIdRequestWithEoriAndNoBadgeId = ConversationIdRequest(conversationId, EventStart, TestFakeRequestWithEoriAndNoBadgeId)
  val TestConversationIdRequestMultipleHeaderValues = ConversationIdRequest(conversationId, EventStart, TestFakeRequestMultipleHeaderValues)
  val TestExtractedHeaders = ExtractedHeadersImpl(VersionOne, ApiSubscriptionFieldsTestData.clientId)
  val TestValidatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)

  val TestValidatedHeadersRequestMultipleHeaderValues: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestMultipleHeaderValues.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestCspAuthorisedRequest: AuthorisedRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(Csp(badgeIdentifier, Some(nrsRetrievalValues)))
  val TestValidatedHeadersRequestNoBadge: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestValidatedHeadersRequestWithBadgeIdAndNoEori: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestWithBadgeIdAndNoEori.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestValidatedHeadersRequestWithEoriAndNoBadgeId: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestWithEoriAndNoBadgeId.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(Csp(badgeIdentifier, Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestCspWithEoriValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(CspWithEori(badgeIdentifier, declarantEori, Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestCspValidatedPayloadRequestMultipleHeaderValues: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequestMultipleHeaderValues.toCspAuthorisedRequest(Csp(badgeIdentifier, Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestNonCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toNonCspAuthorisedRequest(declarantEori, Some(nrsRetrievalValues)).toValidatedPayloadRequest(xmlBody = TestXmlPayload)

  val BatchIdOne = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef001"))
  val BatchIdTwo = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef002"))
  val BatchIdThree = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef003"))
  val FileReferenceOne = FileReference(fromString("31400000-8ce0-11bd-b23e-10b96e4ef00f"))
  val FileReferenceTwo = FileReference(fromString("32400000-8cf0-11bd-b23e-10b96e4ef00f"))
  val FileReferenceThree = FileReference(fromString("33400000-8cd0-11bd-b23e-10b96e4ef00f"))
  val InitiateDateAsString = "2018-04-24T09:30:00Z"
  val InitiateDate = Instant.parse(InitiateDateAsString)
  val CallbackFieldsOne = CallbackFields("name1", "application/xml", "checksum1", InitiateDate, new URL("https://outbound.a.com"))
  val CallbackFieldsTwo = CallbackFields("name2", "application/xml", "checksum2", InitiateDate, new URL("https://outbound.a.com"))
  val CallbackFieldsThree = CallbackFields("name3", "application/xml", "checksum3", InitiateDate, new URL("https://outbound.a.com"))
  val CallbackFieldsUpdated = CallbackFields("UPDATED_NAME", "UPDATED_MIMETYPE", "UPDATED_CHECKSUM", InitiateDate, new URL("https://outbound.a.com"))
  val BatchFileOne = BatchFile(reference = FileReferenceOne, Some(CallbackFieldsOne),
    inboundLocation = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(1), size = 1, documentType = Some(DocumentType("Document Type 1")))
  val BatchFileTwo = BatchFile(reference = FileReferenceTwo, Some(CallbackFieldsTwo),
    inboundLocation = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(2), size = 1, documentType = Some(DocumentType("Document Type 2")))
  val BatchFileThree = BatchFile(reference = FileReferenceThree, Some(CallbackFieldsThree),
    inboundLocation = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(3), size = 1, documentType = Some(DocumentType("Document Type 3")))
  val BatchFileOneNoCallbackFields: BatchFile = BatchFileOne.copy(maybeCallbackFields = None)
  val FileMetadataWithFileOne = FileUploadMetadata(DeclarationId("1"), Eori("123"), csId = subscriptionFieldsId, BatchIdOne, fileCount = 1, Seq(
    BatchFileOne
  ))
  val FileMetadataWithFileTwo = FileUploadMetadata(DeclarationId("2"), Eori("123"), csId = subscriptionFieldsId, BatchIdTwo, fileCount = 1, Seq(
    BatchFileTwo
  ))
  val FileMetadataWithFilesOneAndThree = FileUploadMetadata(DeclarationId("3"), Eori("123"), csId = subscriptionFieldsId, BatchIdThree, fileCount = 2, Seq(
    BatchFileOne, BatchFileThree
  ))
  val FileMetadataWithFileOneWithNoCallbackFieldsAndThree = FileUploadMetadata(DeclarationId("3"), Eori("123"), csId = subscriptionFieldsId, BatchIdOne, fileCount = 2, Seq(
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
  val CONTENT_TYPE_HEADER_CHARSET: (String, String) = CONTENT_TYPE -> CONTENT_TYPE_CHARSET_VALUE
  val CONTENT_TYPE_HEADER_INVALID: (String, String) = CONTENT_TYPE -> "somethinginvalid"

  val ACCEPT_HMRC_XML_V1_VALUE = "application/vnd.hmrc.1.0+xml"
  val ACCEPT_HMRC_XML_V2_VALUE = "application/vnd.hmrc.2.0+xml"
  val ACCEPT_HMRC_XML_V3_VALUE = "application/vnd.hmrc.3.0+xml"
  val ACCEPT_HMRC_XML_V1_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V1_VALUE
  val ACCEPT_HMRC_XML_V2_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V2_VALUE
  val ACCEPT_HMRC_XML_V3_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V3_VALUE
  val ACCEPT_HEADER_INVALID: (String, String) = ACCEPT -> "invalid"

  val ValidHeadersV2: Map[String, String] = Map(
    CONTENT_TYPE_HEADER,
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

  val ValidHeadersV1: Map[String, String] = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V1_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER,
    X_SUBMITTER_IDENTIFIER_HEADER
  )
}
