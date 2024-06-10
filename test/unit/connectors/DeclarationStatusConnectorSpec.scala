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

package unit.connectors

import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.StatusTestXMLData.expectedDeclarationStatusPayload
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, StatusTestXMLData, TestData}

import scala.concurrent.{ExecutionContext, Future}

class DeclarationStatusConnectorSpec extends AnyWordSpecLike with MockitoSugar with BeforeAndAfterEach with Eventually with Matchers {

  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private val mockWsPost = mock[HttpClient]
  private val mockLogger = stubDeclarationsLogger
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val cdsLogger = mock[CdsLogger]
  private val actorSystem = ActorSystem("mockActorSystem")

  private val declarationsCircuitBreakerConfig: DeclarationsCircuitBreakerConfig = DeclarationsCircuitBreakerConfig(50, 1000, 10000)

  private val connector = new DeclarationStatusConnector(mockWsPost, mockLogger, mockServiceConfigProvider, mockDeclarationsConfigService, cdsLogger, actorSystem)

  private val v2Config = ServiceConfig("v2-url", Some("v2-bearer"), "v2-default")
  private val v3Config = ServiceConfig("v3-url", Some("v3-bearer"), "v3-default")

  private implicit val ar: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, EventStart, VersionTwo,
    ApiSubscriptionFieldsTestData.clientId, Csp(None, Some(badgeIdentifier), None), mock[Request[AnyContent]])

  override protected def beforeEach(): Unit = {
    reset(mockWsPost, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("v2.declaration-status")).thenReturn(v2Config)
    when(mockServiceConfigProvider.getConfig("v3.declaration-status")).thenReturn(v3Config)
    when(mockDeclarationsConfigService.declarationsCircuitBreakerConfig).thenReturn(declarationsCircuitBreakerConfig)
  }

  val successfulResponse: HttpResponse = HttpResponse(200,"")

  "DeclarationStatusConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockWsPost).POSTString(ameq(v2Config.url), anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(successfulResponse))

        awaitRequest

        verify(mockWsPost).POSTString(anyString, ameq(StatusTestXMLData.expectedDeclarationStatusPayload.toString()), any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(successfulResponse))

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
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("declaration-status")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          await(connector.send(expectedDeclarationStatusPayload, date, correlationId, VersionOne))
        }
        caught.getMessage shouldBe "config not found"
      }
    }

    "ensure the date header in passed through in MDG request" in {
       val successfulHttpResponse = HttpResponse(200, "")
      returnResponseForRequest(Future.successful(successfulHttpResponse))

      awaitRequest

      val headersCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      verify(mockWsPost).POSTString(anyString, anyString, headersCaptor.capture())(
        any[HttpReads[HttpResponse]](), any[HeaderCarrier], any[ExecutionContext])
      headersCaptor.getValue should contain(HeaderNames.DATE -> httpFormattedDate)
    }
  }

  private def awaitRequest = {
    await(connector.send(expectedDeclarationStatusPayload, date, correlationId, VersionTwo))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) = {
    when(mockWsPost.POSTString(anyString, anyString, any[SeqOfHeader])(
      any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
