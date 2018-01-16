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

package unit.services

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, verify, verifyZeroInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, MdgWcoDeclarationConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, Ids, RequestedVersion}
import uk.gov.hmrc.customs.declaration.services.{CommunicationService, DateTimeService, UuidService}
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, RequestHeaders}

import scala.concurrent.Future
import scala.xml.NodeSeq

class CommunicationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ApiSubscriptionFieldsTestData {

  private val mockLogger = mock[DeclarationsLogger]
  private val mockMdgWcoDeclarationConnector = mock[MdgWcoDeclarationConnector]
  private val mockApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
  private val mockPayloadDecorator = mock[MdgPayloadDecorator]
  private val mockUuidService = mock[UuidService]
  private val mockDateTimeProvider = mock[DateTimeService]
  private val mockConfiguration = mock[Configuration]
  private val mockHttpResponse = mock[HttpResponse]

  private val correlationId = UUID.randomUUID()
  private val dateTime = new DateTime()
  private val clientIdOverride = s"OVERRIDE_$fieldsIdString"
  private val configKeyPrefix = Some("config-key-prefix")
  private val versionNumber = "version.number"
  private val fullIds = ids.copy(maybeRequestedVersion = Some(RequestedVersion(versionNumber, configKeyPrefix)))
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
    .withExtraHeaders(RequestHeaders.API_SUBSCRIPTION_FIELDS_ID_HEADER, RequestHeaders.ACCEPT_HMRC_XML_V1_HEADER)

  private val expectedApiSubscriptionKey = ApiSubscriptionKey(xClientId, "customs%2Fdeclarations", versionNumber)

  private def testService(test: CommunicationService => Unit) {
    test(new CommunicationService(mockLogger, mockMdgWcoDeclarationConnector, mockApiSubscriptionFieldsConnector,
      mockPayloadDecorator, mockUuidService, mockDateTimeProvider, mockConfiguration))
  }

  override protected def beforeEach(): Unit = {
    reset(mockLogger, mockMdgWcoDeclarationConnector, mockApiSubscriptionFieldsConnector, mockPayloadDecorator, mockUuidService, mockDateTimeProvider, mockConfiguration)
    when(mockUuidService.uuid()).thenReturn(correlationId)
    when(mockDateTimeProvider.nowUtc()).thenReturn(dateTime)
    when(mockConfiguration.getString("override.clientID")).thenReturn(None)
    when(mockMdgWcoDeclarationConnector.send(any[NodeSeq], any[DateTime], any[UUID], any[Option[String]])).thenReturn(mockHttpResponse)
  }

  "CommunicationService" should {
    "send transformed xml to connector" in testService {
      service =>
        setupMockXmlWrapper
        prepareAndSendValidXml(service)
        verify(mockMdgWcoDeclarationConnector).send(meq(WrappedValidXML), any[DateTime], any[UUID], any[Option[String]])
    }

    "generate correlationId and pass to connector" in testService {
      service =>
        setupMockXmlWrapper
        prepareAndSendValidXml(service)
        verify(mockMdgWcoDeclarationConnector).send(any[NodeSeq], any[DateTime], meq(correlationId), any[Option[String]])
    }

    "get utc date time and pass to connector" in testService {
      service =>
        setupMockXmlWrapper
        prepareAndSendValidXml(service)
        verify(mockMdgWcoDeclarationConnector).send(any[NodeSeq], meq(dateTime), any[UUID], any[Option[String]])
    }

    "pass config key prefix to connector" in testService {
      service =>
        setupMockXmlWrapper
        prepareAndSendValidXml(service)
        verify(mockMdgWcoDeclarationConnector).send(any[NodeSeq], any[DateTime], any[UUID], meq(configKeyPrefix))
    }

    "return a generated conversationId and fetched fieldsId" in testService {
      service =>
        setupMockXmlWrapper
        val generatedConversationId = await(prepareAndSendValidXml(service))
        generatedConversationId shouldBe fullIds
    }

    "call payload decorator passing incoming xml" in testService {
      service =>
        prepareAndSendValidXml(service)
        verify(mockPayloadDecorator).wrap(meq(ValidXML), anyString, anyString, any[DateTime])
    }

    "call payload decorator passing api-subscription-fields-id header as clientId" in testService{
      service =>
        prepareAndSendValidXml(service)
        verify(mockPayloadDecorator).wrap(any[NodeSeq], anyString, meq(fieldsIdString), any[DateTime])
        verifyZeroInteractions(mockApiSubscriptionFieldsConnector)
    }

    "when configured, use hardcoded value as clientID instead of api-subscription-fields-id header" in testService {
      service =>
        when(mockConfiguration.getString("override.clientID")).thenReturn(Some(clientIdOverride))

        prepareAndSendValidXml(service)
        verify(mockPayloadDecorator).wrap(any[NodeSeq], anyString, meq(clientIdOverride), any[DateTime])
        verifyZeroInteractions(mockApiSubscriptionFieldsConnector)
    }

    "use fieldsId returned from api subscription service when only X-Client-ID header present." in testService {
      service =>
        val hc = HeaderCarrier().withExtraHeaders(RequestHeaders.X_CLIENT_ID_HEADER)
        when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))

        prepareAndSendValidXml(service, hc)
        verify(mockPayloadDecorator).wrap(any[NodeSeq], anyString, meq(fieldsIdString), any[DateTime])
        verify(mockApiSubscriptionFieldsConnector).getSubscriptionFields(meq(expectedApiSubscriptionKey))(any[HeaderCarrier])
    }

    "when hardcoded value as clientID NOT hardcoded in configuration AND both api-subscription-fields-id and X-Client-ID headers present, use api-subscription-fields-id header" in testService {
      service =>
        val hc = HeaderCarrier().withExtraHeaders(RequestHeaders.API_SUBSCRIPTION_FIELDS_ID_HEADER, RequestHeaders.X_CLIENT_ID_HEADER)
        when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))

        prepareAndSendValidXml(service, hc)
        verify(mockPayloadDecorator).wrap(any[NodeSeq], anyString, meq(fieldsIdString), any[DateTime])
        verifyZeroInteractions(mockApiSubscriptionFieldsConnector)
    }

    "when clientId not specified in configuration or headers then IllegalStateException should be thrown" in testService {
      service =>
        val emptyHeaderCarrier = HeaderCarrier()
        when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))

        val caught = intercept[IllegalStateException](prepareAndSendValidXml(service, emptyHeaderCarrier))
        caught.getMessage shouldBe "No value found for clientId."
    }

    "when hardcoded value as clientID NOT hardcoded in configuration AND api-subscription-fields-id header not present and api subscription service returns failed future" in testService {
      service =>
        val hc = HeaderCarrier().withExtraHeaders(RequestHeaders.X_CLIENT_ID_HEADER)
        when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))

        val caught = intercept[EmulatedServiceFailure](prepareAndSendValidXml(service, hc))
        caught shouldBe emulatedServiceFailure
    }

    "call payload decorator passing conversationId" in testService {
      service =>
        prepareAndSendValidXml(service)
        verify(mockPayloadDecorator).wrap(any[NodeSeq], meq(conversationIdValue), anyString, any[DateTime])
    }

    "call payload decorator passing dateTime" in testService {
      service =>
        prepareAndSendValidXml(service)
        verify(mockPayloadDecorator).wrap(any[NodeSeq], anyString, anyString, meq(dateTime))
    }
  }

  private def setupMockXmlWrapper = {
    when(mockPayloadDecorator.wrap(meq(ValidXML), anyString, anyString, any[DateTime])).thenReturn(WrappedValidXML)
  }

  private def prepareAndSendValidXml(service: CommunicationService, hc: HeaderCarrier = headerCarrier): Ids = {
    await(service.prepareAndSend(ValidXML, fullIds)(hc))
  }
}
