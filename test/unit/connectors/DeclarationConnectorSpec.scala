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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, post, postRequestedFor, urlEqualTo}
import com.github.tomakehurst.wiremock.http.Fault
import org.mockito.Mockito.*
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.declaration.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.connectors.{DeclarationCancellationConnector, DeclarationConnector, DeclarationSubmissionConnector}
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.{CdsLogger, DeclarationsLogger}
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpAuditing, HttpClientV2Provider}
import util.ExternalServicesConfig.{Host, Port}
import util.TestData

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DeclarationConnectorSpec extends AnyWordSpecLike
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with Matchers
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with WireMockSupport
  with HttpClientV2Support
  with ScalaFutures {

  private val mockLogger = mock[DeclarationsLogger]
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val mockDeclarationsCircuitBreakerConfig = mock[DeclarationsCircuitBreakerConfig]
  private val numberOfCallsToTriggerStateChange = 5
  private val unavailablePeriodDurationInMillis = 1000
  private val unstablePeriodDurationInMillis = 10000
  private val mockAuditConnector = mock[AuditConnector]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.http.router" -> "definition.Routes",
      "application.logger.name" -> "customs-declarations",
      "appName" -> "customs-declarations",
      "appUrl" -> "http://customs-wco-declaration.service",
      "auditing.enabled" -> false,
      "auditing.traceRequests" -> false,
      "microservice.services.declaration-cancellation.host" -> Host,
      "microservice.services.declaration-cancellation.port" -> Port,
      "microservice.services.declaration-cancellation.bearer-token" -> "v1-bearer-token",
      "microservice.services.v2.declaration-cancellation.host" -> Host,
      "microservice.services.v2.declaration-cancellation.port" -> Port,
      "microservice.services.v2.declaration-cancellation.bearer-token" -> "v2-bearer-token",
      "microservice.services.v3.declaration-cancellation.host" -> Host,
      "microservice.services.v3.declaration-cancellation.port" -> Port,
      "microservice.services.v3.declaration-cancellation.bearer-token" -> "v3-bearer-token",
      "microservice.services.wco-declaration.host" -> Host,
      "microservice.services.wco-declaration.port" -> Port,
      "microservice.services.wco-declaration.bearer-token" -> "v1-bearer-token",
      "microservice.services.v2.wco-declaration.host" -> Host,
      "microservice.services.v2.wco-declaration.port" -> Port,
      "microservice.services.v2.wco-declaration.bearer-token" -> "v2-bearer-token",
      "microservice.services.v3.wco-declaration.host" -> Host,
      "microservice.services.v3.wco-declaration.port" -> Port,
      "microservice.services.v3.wco-declaration.bearer-token" -> "v3-bearer-token",
    ).overrides(
      bind[HttpAuditing].to[DefaultHttpAuditing],
      bind[String].qualifiedWith("appName").toInstance("customs-declarations"),
      bind[HttpClientV2].toProvider[HttpClientV2Provider],
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[ServiceConfigProvider].toInstance(mockServiceConfigProvider),
      bind[DeclarationsConfigService].toInstance(mockDeclarationsConfigService),
      bind[DeclarationsLogger].toInstance(mockLogger)
    ).build()

  lazy val cancellationConnector: DeclarationCancellationConnector = app.injector.instanceOf[DeclarationCancellationConnector]
  lazy val submissionConnector: DeclarationSubmissionConnector = app.injector.instanceOf[DeclarationSubmissionConnector]

  private val v1Config = ServiceConfig(s"http://$Host:$Port/declarations/submitdeclaration", Some("v1-bearer-token"), "v1-default")
  private val v2Config = ServiceConfig(s"http://$Host:$Port/declarations/submitdeclaration", Some("v2-bearer-token"), "v2-default")
  private val v3Config = ServiceConfig(s"http://$Host:$Port/declarations/submitdeclaration", Some("v3-bearer-token"), "v3-default")

  private val xml = <xml></xml>

  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def beforeEach(): Unit = {
    reset(mockLogger, mockServiceConfigProvider)
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
    when(mockServiceConfigProvider.getConfig("wco-declaration")).thenReturn(v1Config)
    when(mockServiceConfigProvider.getConfig("v2.wco-declaration")).thenReturn(v2Config)
    when(mockServiceConfigProvider.getConfig("v3.wco-declaration")).thenReturn(v3Config)
    when(mockServiceConfigProvider.getConfig("declaration-cancellation")).thenReturn(v1Config)
    when(mockServiceConfigProvider.getConfig("v2.declaration-cancellation")).thenReturn(v2Config)
    when(mockServiceConfigProvider.getConfig("v3.declaration-cancellation")).thenReturn(v3Config)
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
  private val date = LocalDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour).toInstant(ZoneOffset.UTC)

  private val httpFormattedDate = "Tue, 04 Jul 2017 13:45:00 UTC"

  private val correlationId = UUID.randomUUID()
  private val successfulHttpResponse = HttpResponse(200, "")

  "MdgWcoDeclarationConnector" can {

    "when making a successful request" should {

      "ensure URL is retrieved from config" in {
        setupSuccessfulDeclarationRequest(v2Config.bearerToken)

        submissionConnector.send(xml, date, correlationId, VersionTwo).futureValue

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/declarations/submitdeclaration"))
          .withHeader(HeaderNames.AUTHORIZATION, equalTo("Bearer v2-bearer-token"))
          .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml; charset=utf-8"))
          .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
          .withHeader(HeaderNames.DATE, equalTo(httpFormattedDate))
          .withHeader(HeaderNames.X_FORWARDED_HOST, equalTo("MDTP"))
          .withHeader("X-Correlation-ID", equalTo(correlationId.toString))
          .withRequestBody(equalTo(xml.toString())))
      }

      "Ensure routing is working for the config location which will ensure version specific config values are loaded correctly" in {
        setupSuccessfulDeclarationRequest(v1Config.bearerToken)
        setupSuccessfulDeclarationRequest(v2Config.bearerToken)
        setupSuccessfulDeclarationRequest(v3Config.bearerToken)

        await(submissionConnector.send(xml, date, correlationId, VersionThree))
        verify(mockServiceConfigProvider).getConfig("v3.wco-declaration")

        await(submissionConnector.send(xml, date, correlationId, VersionTwo))
        verify(mockServiceConfigProvider).getConfig("v2.wco-declaration")

        await(submissionConnector.send(xml, date, correlationId, VersionOne))
        verify(mockServiceConfigProvider).getConfig("wco-declaration")

        await(cancellationConnector.send(xml, date, correlationId, VersionThree))
        verify(mockServiceConfigProvider).getConfig("v3.declaration-cancellation")

        await(cancellationConnector.send(xml, date, correlationId, VersionTwo))
        verify(mockServiceConfigProvider).getConfig("v2.declaration-cancellation")

        await(cancellationConnector.send(xml, date, correlationId, VersionOne))
        verify(mockServiceConfigProvider).getConfig("declaration-cancellation")
      }
    }

    "propagate an underlying error when api subscription fields call fails with a non-http exception" in {
      wireMockServer.stubFor(post(urlEqualTo("/declarations/submitdeclaration"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(s"Bearer ${v2Config.bearerToken.get}"))
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml; charset=utf-8"))
        .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
        .withHeader(HeaderNames.DATE, equalTo(httpFormattedDate))
        .withHeader(HeaderNames.X_FORWARDED_HOST, equalTo("MDTP"))
        .withHeader("X-Correlation-ID", equalTo(correlationId.toString))
        .withRequestBody(equalTo(xml.toString()))
        .willReturn(
          aResponse()
            .withFault(Fault.CONNECTION_RESET_BY_PEER)))

      val caught = intercept[TestData.ConnectionResetFailure] {
        await(submissionConnector.send(xml, date, correlationId, VersionTwo))
      }

      caught.getCause shouldBe TestData.connectionResetFailure.getCause
    }

    "return the http exception when http call fails with an http exception" in {
      wireMockServer.stubFor(post(urlEqualTo("/declarations/submitdeclaration"))
        .withHeader(HeaderNames.AUTHORIZATION, equalTo(s"Bearer ${v2Config.bearerToken.get}"))
        .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml; charset=utf-8"))
        .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
        .withHeader(HeaderNames.DATE, equalTo(httpFormattedDate))
        .withHeader(HeaderNames.X_FORWARDED_HOST, equalTo("MDTP"))
        .withHeader("X-Correlation-ID", equalTo(correlationId.toString))
        .withRequestBody(equalTo(xml.toString()))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)))

      val caught = intercept[Non2xxResponseException] {
        await(submissionConnector.send(xml, date, correlationId, VersionTwo))
      }
      wireMockServer.verify(1, postRequestedFor(urlEqualTo("/declarations/submitdeclaration")))
      caught.message shouldBe s"""Call to Declarations backend failed. Status=[500] url=[http://localhost:$Port/declarations/submitdeclaration] response body=[<empty>]"""
    }

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("v2.wco-declaration")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          await(submissionConnector.send(xml, date, correlationId, VersionTwo))
        }
        caught.getMessage shouldBe "config not found"
      }
    }
  }

  private def setupSuccessfulDeclarationRequest(bearerToken: Option[String]): Unit = {
    wireMockServer.stubFor(post(urlEqualTo("/declarations/submitdeclaration"))
      .withHeader(HeaderNames.AUTHORIZATION, equalTo(s"Bearer ${bearerToken.get}"))
      .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/xml; charset=utf-8"))
      .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.XML))
      .withHeader(HeaderNames.DATE, equalTo(httpFormattedDate))
      .withHeader(HeaderNames.X_FORWARDED_HOST, equalTo("MDTP"))
      .withHeader("X-Correlation-ID", equalTo(correlationId.toString))
      .withRequestBody(equalTo(xml.toString()))
      .willReturn(
        aResponse()
          .withStatus(OK)))
  }
}