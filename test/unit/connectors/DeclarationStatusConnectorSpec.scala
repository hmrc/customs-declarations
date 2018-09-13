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

package unit.connectors

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE, DATE, X_FORWARDED_HOST}
import play.api.http.MimeTypes
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AnyContent, AnyContentAsJson, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.connectors.{DeclarationStatusConnector, NrsConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedStatusRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import util.{ApiSubscriptionFieldsTestData, TestData}
import util.TestData.{badgeIdentifier, conversationId, correlationId, nrsConfigEnabled}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class DeclarationStatusConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]

  private val declarationsCircuitBreakerConfig = DeclarationsCircuitBreakerConfig(50, 1000, 10000)

  private val connector = new DeclarationStatusConnector(mockWsPost, mockLogger, mockServiceConfigProvider, mockDeclarationsConfigService)

  private val v2Config = ServiceConfig("v2-url", Some("v2-bearer"), "v2-default")
  private val v3Config = ServiceConfig("v3-url", Some("v3-bearer"), "v3-default")

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val asr = AuthorisedStatusRequest(conversationId, GoogleAnalyticsValues.DeclarationStatus, VersionTwo, badgeIdentifier, ApiSubscriptionFieldsTestData.clientId, mock[Request[AnyContent]])

  private implicit val jsonRequest: ValidatedPayloadRequest[AnyContentAsJson] =  ValidatedPayloadRequest(
    ConversationId(UUID.randomUUID()),
    GoogleAnalyticsValues.Submit,
    VersionTwo,
    ClientId("ABC"),
    NonCsp(Eori("123"), Some(TestData.nonCspRetrievalValues)),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request"))
  )

  private val httpException = new NotFoundException("Emulated 404 response from a web call")

  private val date = DateTime.parse("2018-09-11T10:28:54.128Z")
  private val mrn = Mrn("theMRN")
  private val dmirId = DeclarationManagementInformationRequestId(UUID.randomUUID())

  val expectedDeclarationStatusPayload =
    <n1:queryDeclarationInformationRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/"
    xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns_1="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/"
    xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 request_schema.xsd">
      <n1:requestCommon>
        <n1:clientID>{ApiSubscriptionFieldsTestData.clientId.toString}</n1:clientID>
        <n1:conversationID>{conversationId.toString}</n1:conversationID>
        <n1:correlationID>{correlationId.toString}</n1:correlationID>
        <n1:badgeIdentifier>{badgeIdentifier.toString}</n1:badgeIdentifier>
        <n1:dateTimeStamp>{date.toString}</n1:dateTimeStamp>
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:declarationManagementInformationRequest>
          <tns_1:id>{dmirId.toString}</tns_1:id>
          <tns_1:timeStamp>{date.toString}</tns_1:timeStamp>
          <xsd_1:reference>{mrn.toString}</xsd_1:reference>
        </n1:declarationManagementInformationRequest>
      </n1:requestDetail>
    </n1:queryDeclarationInformationRequest>

  private val expectedHeaderCarrier = HeaderCarrier(extraHeaders = Seq(
    (X_FORWARDED_HOST, "MDTP"),
    ("X-Correlation-ID", correlationId.toString),
    ("X-Conversation-ID", conversationId.toString),
    (DATE, date.toString("EEE, dd MMM yyyy HH:mm:ss z")),
    (CONTENT_TYPE, MimeTypes.XML),
    (ACCEPT, MimeTypes.XML)
  ), authorization = Some(Authorization("Bearer v2-bearer")))

  override protected def beforeEach() {
    reset(mockWsPost, mockLogger, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("v2.declaration-status")).thenReturn(v2Config)
    when(mockServiceConfigProvider.getConfig("v3.declaration-status")).thenReturn(v3Config)
    when(mockDeclarationsConfigService.declarationsCircuitBreakerConfig).thenReturn(declarationsCircuitBreakerConfig)
  }

  "DeclarationStatusConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        verify(mockWsPost).POSTString(ameq(v2Config.url), anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        verify(mockWsPost).POSTString(anyString, ameq(expectedDeclarationStatusPayload.toString()), any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        verify(mockServiceConfigProvider).getConfig("v2.declaration-status")
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when nrs service call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))

        val caught = intercept[TestData.EmulatedServiceFailure] {
          awaitRequest
        }
        caught shouldBe TestData.emulatedServiceFailure
      }

      "wrap an underlying error when nrs service call fails with an http exception" in {
        returnResponseForRequest(Future.failed(httpException))

        val caught = intercept[RuntimeException] {
          awaitRequest
        }
        caught.getCause shouldBe httpException
      }
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("declaration-status")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          await(connector.send(date, correlationId, dmirId, VersionOne, mrn))
        }
        caught.getMessage shouldBe "config not found"
      }
    }
  }

  private def awaitRequest = {
    await(connector.send(date, correlationId, dmirId, VersionTwo, mrn))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) = {
    when(mockWsPost.POSTString(anyString, anyString, any[SeqOfHeader])(
      any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
