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

package util

import java.net.URL
import java.util.UUID
import java.util.UUID.fromString

import com.google.inject.AbstractModule
import org.joda.time.DateTimeZone.UTC
import org.joda.time.{DateTime, LocalDate}
import org.scalatest.mockito.MockitoSugar.mock
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
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ValidatedBatchFileUploadPayloadRequest, _}
import uk.gov.hmrc.customs.declaration.services.{UniqueIdsService, UuidService}
import unit.logging.StubDeclarationsLogger
import util.TestData.declarantEori
import ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.CustomsDeclarationsMetricsTestData.EventStart

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

  val date = DateTime.parse("2018-09-11T10:28:54.128Z")

  val subscriptionFieldsIdString: String = "b82f31c6-2239-4253-b6f5-ed75e37ab7a5"
  val subscriptionFieldsIdUuid: UUID = fromString("b82f31c6-2239-4253-b6f5-ed75e37ab7a5")

  val clientSubscriptionIdString: String = "327d9145-4965-4d28-a2c5-39dedee50334"

  val nrSubmissionIdValue = "902b0150-aa9a-4046-bf27-85889f128c2a"
  val nrSubmissionIdValueUuid: UUID = fromString(nrSubmissionIdValue)
  val nrSubmissionId = NrSubmissionId(nrSubmissionIdValueUuid)
  val nrsConfigEnabled = NrsConfig(nrsEnabled = true, "nrs-api-key", 300)
  val nrsConfigDisabled = NrsConfig(nrsEnabled = false, "nrs-api-key", 300)

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

  val ValidatedBatchFileUploadPayloadRequestForNonCspWithTwoFiles = ValidatedBatchFileUploadPayloadRequest(
    ConversationId(UUID.randomUUID()),
    GoogleAnalyticsValues.Fileupload,
    EventStart,
    VersionTwo,
    ClientId("ABC"),
    NonCsp(Eori("123"), None),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request")),
    BatchFileUploadRequest(DeclarationId("decId123"),FileGroupSize(2),
    Seq(BatchFileUploadFile(FileSequenceNo(1), DocumentType("docType1")), BatchFileUploadFile(FileSequenceNo(2), DocumentType("docType2"))))
  )

  val ValidatedBatchFileUploadPayloadRequestForCspWithTwoFiles = ValidatedBatchFileUploadPayloadRequest(
    ConversationId(UUID.randomUUID()),
    GoogleAnalyticsValues.Fileupload,
    EventStart,
    VersionTwo,
    ClientId("ABC"),
    BatchFileUploadCsp(badgeIdentifier, Eori("123"), None),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request")),
    BatchFileUploadRequest(DeclarationId("decId123"),FileGroupSize(2),
    Seq(BatchFileUploadFile(FileSequenceNo(1), DocumentType("docType1")), BatchFileUploadFile(FileSequenceNo(2), DocumentType("docType2"))))
  )

  val ValidatedBatchFileUploadPayloadRequestWithFourFiles = ValidatedBatchFileUploadPayloadRequest(
    ConversationId(UUID.randomUUID()),
    GoogleAnalyticsValues.Fileupload,
    EventStart,
    VersionTwo,
    ClientId("ABC"),
    NonCsp(Eori("123"), None),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request")),
    BatchFileUploadRequest(
      DeclarationId("decId123"),
      FileGroupSize(4),
      Seq(BatchFileUploadFile(FileSequenceNo(1), DocumentType("docType1")),
        BatchFileUploadFile(FileSequenceNo(2), DocumentType("docType2")),
        BatchFileUploadFile(FileSequenceNo(3), DocumentType("docType3")),
        BatchFileUploadFile(FileSequenceNo(4), DocumentType("docType4"))))
  )

  val nrsInternalIdValue = "internalId"
  val nrsExternalIdValue = "externalId"
  val nrsAgentCodeValue = "agentCode"
  val nrsCredentials = Credentials(providerId= "providerId", providerType= "providerType")
  val nrsConfidenceLevel = L500
  val nrsNinoValue = "ninov"
  val nrsSaUtrValue = "saUtr"
  val nrsNameValue = Name(Some("name"), Some("lastname"))
  val nrsDateOfBirth = Some(LocalDate.now().minusYears(25))
  val nrsEmailValue = Some("nrsEmailValue")
  val nrsAgentInformationValue = AgentInformation(Some("agentId"),
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

  val currentLoginTime: DateTime = new DateTime(1530442800000L, UTC)
  val previousLoginTime: DateTime = new DateTime(1530464400000L, UTC)
  val nrsTimeStamp: DateTime = new DateTime(1530475200000L, UTC)

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

  val nrsRetrievalData = Retrievals.internalId and Retrievals.externalId and Retrievals.agentCode and Retrievals.credentials and Retrievals.confidenceLevel and
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
    "248857ca67c92e1c18459ff287139fd8409372221e32d245ad8cc470dd5c80d5", nrsTimeStamp.toString, nrsRetrievalValues, "bearer-token", Json.parse("""{"Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))))


  val nrsMetadata = new NrsMetadata("cds", "cds-declaration", "application/xml",
    "248857ca67c92e1c18459ff287139fd8409372221e32d245ad8cc470dd5c80d5", nrsTimeStamp.toString, nrsRetrievalValues, "bearer-token", Json.parse("""{"Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))))

  val cspNrsMetadataMultipleHeaderValues = new NrsMetadata("cds", "cds-declaration", "application/xml",
    "248857ca67c92e1c18459ff287139fd8409372221e32d245ad8cc470dd5c80d5", nrsTimeStamp.toString, nrsRetrievalValues, "bearer-token", Json.parse("""{"Accept":"ABC,DEF","Authorization":"bearer-token"}"""),
    JsObject(Map[String, JsValue] ("conversationId" -> JsString(conversationIdValue))))

  val nrsPayload = new NrsPayload("QW55Q29udGVudEFzWG1sKDxmb28+YmFyPC9mb28+KQ==", nrsMetadata)
  val cspNrsPayload = new NrsPayload("QW55Q29udGVudEFzWG1sKDxmb28+YmFyPC9mb28+KQ==", cspNrsMetadata)

  val cspNrsPayloadMultipleHeaderValues = new NrsPayload("QW55Q29udGVudEFzWG1sKDxmb28+YmFyPC9mb28+KQ==", cspNrsMetadataMultipleHeaderValues)

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

  val GoogleAnalyticsPayloadValue = "some-query-string-for-ga"

  lazy val ValidGoogleAnalyticsJson: JsValue = Json.parse(
    s"""
       |{
       | "payload": $GoogleAnalyticsPayloadValue
       |}
    """.stripMargin)

  // note we can not mock service methods that return value classes - however using a simple stub IMHO it results in cleaner code (less mocking noise)
  lazy val stubUniqueIdsService = new UniqueIdsService(mockUuidService) {
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

  // For Status endpoint
  val TestConversationIdStatusRequest = AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.DeclarationStatus, EventStart, TestFakeRequest)
  val TestExtractedStatusHeaders = ExtractedStatusHeadersImpl(VersionTwo, badgeIdentifier, ApiSubscriptionFieldsTestData.clientId)
  val TestValidatedHeadersStatusRequest: ValidatedHeadersStatusRequest[AnyContentAsXml] = TestConversationIdStatusRequest.toValidatedHeadersStatusRequest(TestExtractedStatusHeaders)
  val TestAuthorisedStatusRequest: AuthorisedStatusRequest[AnyContentAsXml] = TestValidatedHeadersStatusRequest.toAuthorisedStatusRequest

  val TestConversationIdRequest = AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, TestFakeRequest)
  val TestConversationIdRequestWithBadgeIdAndNoEori = AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, TestFakeRequestWithBadgeIdAndNoEori)
  val TestConversationIdRequestWithEoriAndNoBadgeId = AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, TestFakeRequestWithEoriAndNoBadgeId)
  val TestConversationIdRequestMultipleHeaderValues = AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, TestFakeRequestMultipleHeaderValues)
  val TestExtractedHeaders = ExtractedHeadersImpl(VersionOne, ApiSubscriptionFieldsTestData.clientId)
  val TestValidatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)

  val TestValidatedHeadersRequestMultipleHeaderValues: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestMultipleHeaderValues.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestCspAuthorisedRequest: AuthorisedRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(Csp(badgeIdentifier, Some(nrsRetrievalValues)))
  val TestValidatedHeadersRequestNoBadge: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestValidatedHeadersRequestWithBadgeIdAndNoEori: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestWithBadgeIdAndNoEori.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestValidatedHeadersRequestWithEoriAndNoBadgeId: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequestWithEoriAndNoBadgeId.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest(Csp(badgeIdentifier, Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestCspValidatedPayloadRequestMultipleHeaderValues: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequestMultipleHeaderValues.toCspAuthorisedRequest(Csp(badgeIdentifier, Some(nrsRetrievalValues))).toValidatedPayloadRequest(xmlBody = TestXmlPayload)
  val TestNonCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toNonCspAuthorisedRequest(declarantEori, Some(nrsRetrievalValues)).toValidatedPayloadRequest(xmlBody = TestXmlPayload)

  val BatchIdOne = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef001"))
  val BatchIdTwo = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef002"))
  val BatchIdThree = BatchId(fromString("48400000-8cf0-11bd-b23e-10b96e4ef003"))
  val FileReferenceOne = FileReference(fromString("31400000-8ce0-11bd-b23e-10b96e4ef00f"))
  val FileReferenceTwo = FileReference(fromString("32400000-8cf0-11bd-b23e-10b96e4ef00f"))
  val FileReferenceThree = FileReference(fromString("33400000-8cd0-11bd-b23e-10b96e4ef00f"))
  val CallbackFieldsOne = CallbackFields("name1", "application/xml", "checksum1")
  val CallbackFieldsTwo = CallbackFields("name2", "application/xml", "checksum2")
  val CallbackFieldsThree = CallbackFields("name3", "application/xml", "checksum3")
  val CallbackFieldsUpdated = CallbackFields("UPDATED_NAME", "UPDATED_MIMETYPE", "UPDATED_CHECKSUM")
  val BatchFileOne = BatchFile(reference = FileReferenceOne, Some(CallbackFieldsOne),
    location = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(1), size = 1, documentType = DocumentType("Document Type 1"))
  val BatchFileTwo = BatchFile(reference = FileReferenceTwo, Some(CallbackFieldsTwo),
    location = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(2), size = 1, documentType = DocumentType("Document Type 2"))
  val BatchFileThree = BatchFile(reference = FileReferenceThree, Some(CallbackFieldsThree),
    location = new URL("https://a.b.com"), sequenceNumber = FileSequenceNo(3), size = 1, documentType = DocumentType("Document Type 3"))
  val BatchFileOneNoCallbackFields = BatchFileOne.copy(maybeCallbackFields = None)
  val BatchFileMetadataWithFileOne = BatchFileUploadMetadata(DeclarationId("1"), Eori("123"), csId = subscriptionFieldsId, BatchIdOne, fileCount = 1, Seq(
    BatchFileOne
  ))
  val BatchFileMetadataWithFileTwo = BatchFileUploadMetadata(DeclarationId("2"), Eori("123"), csId = subscriptionFieldsId, BatchIdTwo, fileCount = 1, Seq(
    BatchFileTwo
  ))
  val BatchFileMetadataWithFilesOneAndThree = BatchFileUploadMetadata(DeclarationId("3"), Eori("123"), csId = subscriptionFieldsId, BatchIdThree, fileCount = 2, Seq(
    BatchFileOne, BatchFileThree
  ))
  val BatchFileMetadataWithFileOneWithNoCallbackFieldsAndThree = BatchFileUploadMetadata(DeclarationId("3"), Eori("123"), csId = subscriptionFieldsId, BatchIdOne, fileCount = 2, Seq(
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

  val ValidHeadersV2 = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V2_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER
  )

  val ValidHeadersV3 = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V3_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER
  )

  val ValidHeadersV1 = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V1_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER
  )

  val ValidGoogleAnalyticsHeaders = Map(
    CONTENT_TYPE -> MimeTypes.JSON,
    ACCEPT_HMRC_XML_V1_HEADER
  )
}
