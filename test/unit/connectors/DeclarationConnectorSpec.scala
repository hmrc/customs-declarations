/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.actor.ActorSystem
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.DeclarationConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import util.UnitSpec
import util.TestData

import scala.concurrent.{ExecutionContext, Future}

class DeclarationConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val mockDeclarationsCircuitBreakerConfig = mock[DeclarationsCircuitBreakerConfig]
  private val numberOfCallsToTriggerStateChange = 5
  private val unavailablePeriodDurationInMillis = 1000
  private val unstablePeriodDurationInMillis = 10000
  private val mockCdsLogger = mock[CdsLogger]
  private val mockActorSystem = ActorSystem("mockActorSystem")

  class DummyDeclarationCancellationConnector (
      override val http: HttpClient,
      override val logger: DeclarationsLogger,
      override val serviceConfigProvider: ServiceConfigProvider,
      override val config: DeclarationsConfigService,
      override val cdsLogger: CdsLogger,
      override val actorSystem: ActorSystem)
    (implicit val ec: ExecutionContext) extends DeclarationConnector {
    override val configKey = "wco-declaration"
  }

  lazy val connector = new DummyDeclarationCancellationConnector(mockWsPost, mockLogger, mockServiceConfigProvider, mockDeclarationsConfigService, mockCdsLogger, mockActorSystem)(Helpers.stubControllerComponents().executionContext)

  private val v1Config = ServiceConfig("v1-url", Some("v1-bearer-token"), "v1-default")
  private val v2Config = ServiceConfig("v2-url", Some("v2-bearer-token"), "v2-default")
  private val v3Config = ServiceConfig("v3-url", Some("v3-bearer-token"), "v3-default")

  private val xml = <xml></xml>

  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest

  override protected def beforeEach() {
    reset(mockWsPost, mockLogger, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("wco-declaration")).thenReturn(v1Config)
    when(mockServiceConfigProvider.getConfig("v2.wco-declaration")).thenReturn(v2Config)
    when(mockServiceConfigProvider.getConfig("v3.wco-declaration")).thenReturn(v3Config)
    when(mockDeclarationsConfigService.declarationsCircuitBreakerConfig).thenReturn(mockDeclarationsCircuitBreakerConfig)
    when(mockDeclarationsCircuitBreakerConfig.numberOfCallsToTriggerStateChange).thenReturn(numberOfCallsToTriggerStateChange)
    when(mockDeclarationsCircuitBreakerConfig.unavailablePeriodDurationInMillis).thenReturn(unavailablePeriodDurationInMillis)
    when(mockDeclarationsCircuitBreakerConfig.unstablePeriodDurationInMillis).thenReturn(unstablePeriodDurationInMillis)
  }

  private val year = 2017
  private val monthOfYear = 7
  private val dayOfMonth = 4
  private val hourOfDay = 13
  private val minuteOfHour = 45
  private val date = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, DateTimeZone.UTC)

  private val httpFormattedDate = "Tue, 04 Jul 2017 13:45:00 UTC"

  private val correlationId = UUID.randomUUID()
  private val successfulHttpResponse = HttpResponse(200, "")

  "MdgWcoDeclarationConnector" can {

    "when making a successful request" should {

      "ensure URL is retrieved from config" in {
        returnResponseForRequest(Future.successful(successfulHttpResponse))

        awaitRequest()

        verify(mockWsPost).POSTString(ameq(v2Config.url), anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "ensure xml payload is included in the MDG request body" in {
        returnResponseForRequest(Future.successful(successfulHttpResponse))

        awaitRequest()

        verify(mockWsPost).POSTString(anyString, ameq(xml.toString()), any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "ensure the content type header in passed through in MDG request" in {
        returnResponseForRequest(Future.successful(successfulHttpResponse))

        awaitRequest()

        val headersCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        verify(mockWsPost).POSTString(anyString, anyString, headersCaptor.capture())(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier], any[ExecutionContext])
        headersCaptor.getValue should contain(HeaderNames.CONTENT_TYPE -> "application/xml; charset=utf-8")
      }

      "ensure the accept header in passed through in MDG request" in {
        returnResponseForRequest(Future.successful(successfulHttpResponse))

        awaitRequest()

        val headersCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        verify(mockWsPost).POSTString(anyString, anyString, headersCaptor.capture())(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier], any[ExecutionContext])
        headersCaptor.getValue should contain(HeaderNames.ACCEPT -> MimeTypes.XML)
      }

      "ensure the date header in passed through in MDG request" in {
        returnResponseForRequest(Future.successful(successfulHttpResponse))

        awaitRequest()

        val headersCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        verify(mockWsPost).POSTString(anyString, anyString, headersCaptor.capture())(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier], any[ExecutionContext])
        headersCaptor.getValue should contain(HeaderNames.DATE -> httpFormattedDate)
      }

      "ensure the X-FORWARDED_HOST header in passed through in MDG request" in {
        returnResponseForRequest(Future.successful(successfulHttpResponse))

        awaitRequest()

        val headersCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        verify(mockWsPost).POSTString(anyString, anyString, headersCaptor.capture())(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier], any[ExecutionContext])
        headersCaptor.getValue should contain(HeaderNames.X_FORWARDED_HOST -> "MDTP")
      }

      "ensure the X-Correlation-Id header in passed through in MDG request" in {
        returnResponseForRequest(Future.successful(successfulHttpResponse))

        awaitRequest()

        val headersCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        verify(mockWsPost).POSTString(anyString, anyString, headersCaptor.capture())(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier], any[ExecutionContext])
        headersCaptor.getValue should contain("X-Correlation-ID" -> correlationId.toString)
      }

      "Ensure routing is working for the config location which will ensure version specific config values are loaded correctly" in {
        returnResponseForRequest(Future.successful(successfulHttpResponse))

        await(connector.send(xml, date, correlationId, VersionThree))

        verify(mockServiceConfigProvider).getConfig("v3.wco-declaration")

        await(connector.send(xml, date, correlationId, VersionTwo))

        verify(mockServiceConfigProvider).getConfig("v2.wco-declaration")

        await(connector.send(xml, date, correlationId, VersionOne))

        verify(mockServiceConfigProvider).getConfig("wco-declaration")
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when MDG call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))

        val caught = intercept[TestData.EmulatedServiceFailure] {
          awaitRequest()
        }
        caught shouldBe TestData.emulatedServiceFailure
      }
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("v2.wco-declaration")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          awaitRequest()
        }
        caught.getMessage shouldBe "config not found"
      }
    }
  }

  private def awaitRequest() = {
    await(connector.send(xml, date, correlationId, VersionTwo))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) = {
    when(mockWsPost.POSTString(anyString, anyString, any[SeqOfHeader])(
      any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
