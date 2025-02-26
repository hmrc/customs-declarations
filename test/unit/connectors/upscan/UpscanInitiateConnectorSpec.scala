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

package unit.connectors.upscan

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, post, postRequestedFor, urlEqualTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import play.mvc.Http.HeaderNames.*
import uk.gov.hmrc.customs.declaration.connectors.upscan.UpscanInitiateConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedFileUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpAuditing, HttpClientV2Provider}
import util.ExternalServicesConfig.{Host, Port}
import util.TestData
import util.TestData.{ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles, fileUploadConfig}
import util.VerifyLogging.verifyDeclarationsLoggerError

class UpscanInitiateConnectorSpec extends AnyWordSpecLike
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with Matchers
  with WireMockSupport
  with ScalaFutures
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with HttpClientV2Support {

  private implicit val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val mockAuditConnector = mock[AuditConnector]
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val tenThousand: Int = 10000
  private val upscanInitiatePayloadV1WithNoRedirects: UpscanInitiatePayload = UpscanInitiatePayload("https://callbackurl.com", tenThousand, None, None)
  private val upscanInitiatePayloadV1WithSuccessRedirects: UpscanInitiatePayload = UpscanInitiatePayload("https://callbackurl.com", tenThousand, Some("https://success-redirect.com"), None)
  private val upscanInitiatePayloadV1WithErrorRedirects: UpscanInitiatePayload = UpscanInitiatePayload("https://callbackurl.com", tenThousand, None, Some("https://error-redirect.com"))
  private val upscanInitiatePayloadV2: UpscanInitiatePayload = UpscanInitiatePayload("https://callbackurl.com", tenThousand, Some("https://success-redirect.com"), Some("https://error-redirect.com"))

  implicit val jsonRequest: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.http.router" -> "definition.Routes",
      "application.logger.name" -> "customs-declarations",
      "appName" -> "customs-declarations",
      "appUrl" -> "http://customs-wco-declaration.service",
      "auditing.enabled" -> false,
      "auditing.traceRequests" -> false,
      "microservice.services.upscan-initiate-v1.host" -> Host,
      "microservice.services.upscan-initiate-v1.port" -> Port,
      "microservice.services.upscan-initiate-v2.host" -> Host,
      "microservice.services.upscan-initiate-v2.port" -> Port,
    ).overrides(
      bind[HttpAuditing].to[DefaultHttpAuditing],
      bind[String].qualifiedWith("appName").toInstance("customs-declarations"),
      bind[HttpClientV2].toProvider[HttpClientV2Provider],
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[DeclarationsConfigService].toInstance(mockDeclarationsConfigService),
      bind[DeclarationsLogger].toInstance(mockLogger)
    ).build()

  lazy val connector: UpscanInitiateConnector = app.injector.instanceOf[UpscanInitiateConnector]

  override protected def beforeEach(): Unit = {
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
    reset(mockLogger)
    when(mockDeclarationsConfigService.fileUploadConfig).thenReturn(fileUploadConfig)
  }

  "UpscanInitiateConnector" can {

    "when making a successful request" should {

      "select V1 URL from config when no redirect values are present" in {
        wireMockServer.stubFor(post(urlEqualTo("/upscan/initiate"))
          .withHeader(CONTENT_TYPE, equalTo("application/json"))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(upscanInitiatePayloadV1WithNoRedirects))))
          .willReturn(
            aResponse()
              .withBody("""{"reference":"string", "uploadRequest":{"href":"", "fields": {"test":"test"}}}""")
              .withStatus(OK)))

        awaitRequest(upscanInitiatePayloadV1WithNoRedirects)

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/upscan/initiate")))
      }

      "select V1 URL from config when success redirect and no error redirect values are present" in {
        wireMockServer.stubFor(post(urlEqualTo("/upscan/initiate"))
          .withHeader(CONTENT_TYPE, equalTo("application/json"))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(upscanInitiatePayloadV1WithSuccessRedirects))))
          .willReturn(
            aResponse()
              .withBody("""{"reference":"string", "uploadRequest":{"href":"", "fields": {"test":"test"}}}""")
              .withStatus(OK)))

        awaitRequest(upscanInitiatePayloadV1WithSuccessRedirects)

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/upscan/initiate")))
      }

      "select V1 URL from config when error redirect and no success redirect values are present" in {
        wireMockServer.stubFor(post(urlEqualTo("/upscan/initiate"))
          .withHeader(CONTENT_TYPE, equalTo("application/json"))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(upscanInitiatePayloadV1WithErrorRedirects))))
          .willReturn(
            aResponse()
              .withBody("""{"reference":"string", "uploadRequest":{"href":"", "fields": {"test":"test"}}}""")
              .withStatus(OK)))

        awaitRequest(upscanInitiatePayloadV1WithErrorRedirects)
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/upscan/initiate")))
      }

      "select V2 URL from config when both redirect values are present" in {
        wireMockServer.stubFor(post(urlEqualTo("/upscan/v2/initiate"))
          .withHeader(CONTENT_TYPE, equalTo("application/json"))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withRequestBody(
            equalToJson(Json.stringify(Json.toJson(upscanInitiatePayloadV2))))
          .willReturn(
            aResponse()
              .withBody("""{"reference":"string", "uploadRequest":{"href":"", "fields": {"test":"test"}}}""")
              .withStatus(OK)))

        awaitRequest()

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/upscan/v2/initiate")))
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when MDG call fails with a non-http exception" in {
        wireMockServer.stubFor(post(urlEqualTo("/upscan/v2/initiate"))
          .withHeader(CONTENT_TYPE, equalTo("application/json"))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(upscanInitiatePayloadV2))))
          .willReturn(
            aResponse()
              .withFixedDelay(60000)))

        val caught = intercept[TestData.TimeoutExceptionFailure] {
          awaitRequest()
        }

        caught.getCause shouldBe TestData.timeoutExceptionFailure.getCause
        verifyDeclarationsLoggerError("Call to upscan initiate failed.")
      }

      "return the http exception when MDG call fails with an http exception" in {
        wireMockServer.stubFor(post(urlEqualTo("/upscan/v2/initiate"))
          .withHeader(CONTENT_TYPE, equalTo("application/json"))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withRequestBody(equalToJson(Json.stringify(Json.toJson(upscanInitiatePayloadV2))))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)))

        val caught = intercept[UpstreamErrorResponse] {
          awaitRequest()
        }
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/upscan/v2/initiate")))
        caught.message shouldBe "POST of 'http://localhost:6001/upscan/v2/initiate' returned 500. Response body: ''"
      }
    }
  }

  private def awaitRequest(payload: UpscanInitiatePayload = upscanInitiatePayloadV2): UpscanInitiateResponsePayload = {
    await(connector.send(payload, VersionOne))
  }
}
