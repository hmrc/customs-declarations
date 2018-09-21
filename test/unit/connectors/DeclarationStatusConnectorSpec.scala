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

import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsJson, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedStatusRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, TestData, TestXMLData}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class DeclarationStatusConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = stubDeclarationsLogger
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

  override protected def beforeEach() {
    reset(mockWsPost, mockServiceConfigProvider)
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

        verify(mockWsPost).POSTString(anyString, ameq(TestXMLData.expectedDeclarationStatusPayload.toString()), any[SeqOfHeader])(
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
