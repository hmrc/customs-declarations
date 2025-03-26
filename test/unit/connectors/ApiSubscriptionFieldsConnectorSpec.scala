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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, getRequestedFor, urlEqualTo}
import com.github.tomakehurst.wiremock.http.Fault
import org.mockito.Mockito.*
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers
import play.api.test.Helpers.{ACCEPT, await, defaultAwaitTimeout}
import play.api.inject.bind
import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpAuditing, HttpClientV2Provider}
import util.CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext
import util.ExternalServicesConfig.*
import util.{ApiSubscriptionFieldsTestData, TestData}

import scala.concurrent.ExecutionContext

class ApiSubscriptionFieldsConnectorSpec extends AnyWordSpecLike
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with ApiSubscriptionFieldsTestData
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with HttpClientV2Support
  with WireMockSupport {

  private val mockLogger = mock[DeclarationsLogger]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val mockDeclarationsConfig = mock[DeclarationsConfig]
  private val mockAuditConnector = mock[AuditConnector]
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.http.router" -> "definition.Routes",
      "application.logger.name" -> "customs-declarations",
      "appName" -> "customs-declarations",
      "appUrl" -> "http://customs-wco-declaration.service",
      "auditing.enabled" -> false,
      "auditing.traceRequests" -> false,
      "microservice.services.api-subscription-fields" -> Host,
      "microservice.services.api-subscription-fields" -> Port,
    ).overrides(
      bind[HttpAuditing].to[DefaultHttpAuditing],
      bind[String].qualifiedWith("appName").toInstance("customs-declarations"),
      bind[HttpClientV2].toProvider[HttpClientV2Provider],
      bind[AuditConnector].toInstance(mockAuditConnector),
      bind[DeclarationsConfigService].toInstance(mockDeclarationsConfigService),
      bind[DeclarationsLogger].toInstance(mockLogger)
    ).build()

  private val connector: ApiSubscriptionFieldsConnector = app.injector.instanceOf[ApiSubscriptionFieldsConnector]
  private val expectedUrl = s"$ApiSubscriptionFieldsContext/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0"
  
  override protected def beforeEach(): Unit = {
    reset(mockLogger, mockDeclarationsConfigService)
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
    when(mockDeclarationsConfigService.declarationsConfig).thenReturn(mockDeclarationsConfig)
    when(mockDeclarationsConfig.apiSubscriptionFieldsBaseUrl).thenReturn(s"http://$Host:$Port$ApiSubscriptionFieldsContext")
  }

  "ApiSubscriptionFieldsConnector" can {
    "when making a successful request" should {
      "use the correct URL for valid path parameters and config" in {
        wireMockServer.stubFor(get(urlEqualTo(expectedUrl))
          .withHeader(ACCEPT, equalTo("*/*"))
          .willReturn(
            aResponse()
              .withBody(Json.stringify(Json.toJson(apiSubscriptionFieldsResponse)))
              .withStatus(OK)))

        awaitRequest shouldBe apiSubscriptionFieldsResponse
        wireMockServer.verify(1, getRequestedFor(urlEqualTo(expectedUrl)))
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when api subscription fields call fails with a non-http exception" in {
        wireMockServer.stubFor(get(urlEqualTo(expectedUrl))
          .withHeader(ACCEPT, equalTo("*/*"))
          .willReturn(
            aResponse()
              .withFault(Fault.CONNECTION_RESET_BY_PEER)))

        val caught = intercept[TestData.ConnectionResetFailure] {
          awaitRequest
        }

        caught.getCause shouldBe TestData.connectionResetFailure.getCause
      }

      "return the http exception when http call fails with an http exception" in {
        wireMockServer.stubFor(get(urlEqualTo(expectedUrl))
          .withHeader(ACCEPT, equalTo("*/*"))
          .willReturn(
            aResponse()
              .withBody(Json.stringify(Json.toJson(apiSubscriptionFieldsResponse)))
              .withStatus(INTERNAL_SERVER_ERROR)))

        val caught = intercept[UpstreamErrorResponse] {
          awaitRequest
        }
        wireMockServer.verify(1, getRequestedFor(urlEqualTo(expectedUrl)))
        caught.message shouldBe s"""GET of 'http://localhost:$Port/api-subscription-fields/field/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0' returned 500. Response body: '{"fieldsId":"327d9145-4965-4d28-a2c5-39dedee50334","fields":{"authenticatedEori":"ZZ123456789000"}}'"""
      }
    }
  }

  private def awaitRequest = {
    await(connector.getSubscriptionFields(apiSubscriptionKey))
  }
}
