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

import java.util.UUID

import com.google.inject.AbstractModule
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ConversationIdRequest, ExtractedHeadersImpl, ValidatedHeadersRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services.{UniqueIdsService, UuidService}

import scala.xml.Elem

object TestData {
  val conversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val conversationIdUuid: UUID = UUID.fromString(conversationIdValue)
  val conversationId: ConversationId = ConversationId(conversationIdUuid)

  val correlationIdValue = "e61f8eee-812c-4b8f-b193-06aedc60dca2"
  val correlationIdUuid: UUID = UUID.fromString(correlationIdValue)
  val correlationId = CorrelationId(correlationIdUuid)

  val validBadgeIdentifierValue = "BADGEID123"
  val invalidBadgeIdentifierValue = "INVALIDBADGEID123456789"
  val invalidBadgeIdentifier: BadgeIdentifier = BadgeIdentifier(invalidBadgeIdentifierValue)
  val badgeIdentifier: BadgeIdentifier = BadgeIdentifier(validBadgeIdentifierValue)

  val cspBearerToken = "CSP-Bearer-Token"
  val nonCspBearerToken = "Software-House-Bearer-Token"

  val declarantEoriValue = "ZZ123456789000"
  val declarantEori = Eori(declarantEoriValue)

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  val mockUuidService: UuidService = MockitoSugar.mock[UuidService]

  object TestModule extends AbstractModule {
    def configure(): Unit = {
      bind(classOf[UuidService]) toInstance mockUuidService
    }

    def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
  }

  // note we can not mock service methods that return value classes - however using a simple stub IMHO it results in cleaner code (less mocking noise)
  val stubUniqueIdsService = new UniqueIdsService(mockUuidService) {
    override def conversation: ConversationId = conversationId
    override def correlation: CorrelationId = correlationId
  }

  val TestXmlPayload: Elem = <foo>bar</foo>
  val TestFakeRequest: FakeRequest[AnyContentAsXml] = FakeRequest().withXmlBody(TestXmlPayload)
  val TestConversationIdRequest = ConversationIdRequest(conversationId, TestFakeRequest)
  val TestExtractedHeaders = ExtractedHeadersImpl(Some(badgeIdentifier), VersionOne, ApiSubscriptionFieldsTestData.clientId)
  val TestExtractedHeadersNoBadge: ExtractedHeadersImpl = TestExtractedHeaders.copy(maybeBadgeIdentifier = None)
  val TestValidatedHeadersRequest: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeaders)
  val TestValidatedHeadersRequestNoBadge: ValidatedHeadersRequest[AnyContentAsXml] = TestConversationIdRequest.toValidatedHeadersRequest(TestExtractedHeadersNoBadge)
  val TestCspValidatedPayloadRequest: ValidatedPayloadRequest[AnyContentAsXml] = TestValidatedHeadersRequest.toCspAuthorisedRequest.toValidatedPayloadRequest(xmlBody = TestXmlPayload)

}

object RequestHeaders {

  val X_CONVERSATION_ID_NAME = "X-Conversation-ID"
  val X_CONVERSATION_ID_HEADER: (String, String) = X_CONVERSATION_ID_NAME -> TestData.conversationId.toString

  val X_BADGE_IDENTIFIER_NAME = "X-Badge-Identifier"
  val X_BADGE_IDENTIFIER_HEADER: (String, String) = X_BADGE_IDENTIFIER_NAME -> TestData.badgeIdentifier.value
  val X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG: (String, String) = X_BADGE_IDENTIFIER_NAME -> TestData.invalidBadgeIdentifierValue
  val X_BADGE_IDENTIFIER_HEADER_INVALID_CHARS: (String, String) = X_BADGE_IDENTIFIER_NAME -> "Invalid^&&("
  val X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_SHORT: (String, String) = X_BADGE_IDENTIFIER_NAME -> "12345"
  val X_BADGE_IDENTIFIER_HEADER_INVALID_LOWERCASE: (String, String) = X_BADGE_IDENTIFIER_NAME -> "BadgeId123"

  val X_CLIENT_ID_ID_NAME = "X-Client-ID"
  val X_CLIENT_ID_HEADER: (String, String) = X_CLIENT_ID_ID_NAME -> ApiSubscriptionFieldsTestData.xClientId
  val X_CLIENT_ID_HEADER_INVALID: (String, String) = X_CLIENT_ID_ID_NAME -> "This is not a UUID"

  val CONTENT_TYPE_HEADER: (String, String) = CONTENT_TYPE -> MimeTypes.XML
  val CONTENT_TYPE_CHARSET_VALUE: String = s"${MimeTypes.XML}; charset=UTF-8"
  val CONTENT_TYPE_HEADER_CHARSET: (String, String) = CONTENT_TYPE -> CONTENT_TYPE_CHARSET_VALUE
  val CONTENT_TYPE_HEADER_INVALID: (String, String) = CONTENT_TYPE -> "somethinginvalid"

  val ACCEPT_HMRC_XML_V1_VALUE = "application/vnd.hmrc.1.0+xml"
  val ACCEPT_HMRC_XML_V2_VALUE = "application/vnd.hmrc.2.0+xml"
  val ACCEPT_HMRC_XML_V1_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V1_VALUE
  val ACCEPT_HMRC_XML_V2_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V2_VALUE
  val ACCEPT_HEADER_INVALID: (String, String) = ACCEPT -> "invalid"

  val AUTH_HEADER_VALUE: String = "AUTH_HEADER_VALUE"
  val AUTH_HEADER: (String, String) = HeaderNames.AUTHORIZATION -> AUTH_HEADER_VALUE

  val ValidHeadersV2 = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V2_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER
  )

  val ValidHeadersV1 = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V1_HEADER,
    X_CLIENT_ID_HEADER,
    X_BADGE_IDENTIFIER_HEADER
  )
}
