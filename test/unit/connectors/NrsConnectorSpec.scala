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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, get, post, postRequestedFor, urlEqualTo}
import org.mockito.ArgumentMatchers.{eq as ameq, *}
import org.mockito.Mockito.*
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.MimeTypes
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.libs.json.{Json, Writes}
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers.{ACCEPT, CONTENT_TYPE, await, defaultAwaitTimeout}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declaration.connectors.NrsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpAuditing, HttpClientV2Provider}
import util.CustomsDeclarationsExternalServicesConfig.NrsServiceContext
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.TestData
import util.TestData.{fileUploadConfig, nrSubmissionId, nrsConfigEnabled}
import util.ExternalServicesConfig.*

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class NrsConnectorSpec extends AnyWordSpecLike
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with Matchers
  with BeforeAndAfterAll
  with GuiceOneAppPerSuite
  with WireMockSupport
  with HttpClientV2Support {

  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val mockWsPost = mock[HttpClientV2]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val mockAuditConnector = mock[AuditConnector]

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

  private val connector: NrsConnector = app.injector.instanceOf[NrsConnector]

  private implicit val jsonRequest: ValidatedPayloadRequest[AnyContentAsJson] =  ValidatedPayloadRequest(
    ConversationId(UUID.randomUUID()),
    EventStart,
    VersionTwo,
    ClientId("ABC"),
    NonCsp(Eori("123"), Some(TestData.nrsRetrievalValues)),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request"))
  )

  private val httpException =  UpstreamErrorResponse("Emulated 404 response from a web call", 404)

  override protected def beforeEach(): Unit = {
    reset(mockWsPost, mockLogger)
    wireMockServer.resetMappings()
    wireMockServer.resetRequests()
    when(mockDeclarationsConfigService.fileUploadConfig).thenReturn(fileUploadConfig)
    when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
  }

  "NrsConnector" can {
    "when making a successful request" should {
      "pass URL from config, with request body and extra headers" in {
        wireMockServer.stubFor(post(urlEqualTo("/submission"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withHeader("X-API-Key", equalTo("nrs-api-key"))
          .withRequestBody(equalTo(Json.stringify(Json.toJson(TestData.nrsPayload))))
          .willReturn(
            aResponse()
              .withBody(Json.stringify(Json.toJson(nrSubmissionId)))
              .withStatus(OK)))

        awaitRequest shouldBe nrSubmissionId

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/submission"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withHeader("X-API-Key", equalTo("nrs-api-key"))
          .withRequestBody(equalTo(Json.stringify(Json.toJson(TestData.nrsPayload))))
        )
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when nrs service call fails with a non-http exception" in {
//        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))
//
//        val caught = intercept[TestData.EmulatedServiceFailure] {
//          awaitRequest
//        }
//        caught shouldBe TestData.emulatedServiceFailure
      }

      "wrap an underlying error when nrs service call fails with an http exception" in {
        wireMockServer.stubFor(post(urlEqualTo("/submission"))
          .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withHeader(ACCEPT, equalTo("*/*"))
          .withHeader("X-API-Key", equalTo("nrs-api-key"))
          .withRequestBody(equalTo(Json.stringify(Json.toJson(TestData.nrsPayload))))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)))

        val caught = intercept[UpstreamErrorResponse] {
          awaitRequest
        }
        caught.message shouldBe s"POST of 'http://localhost:$Port/submission' returned 500. Response body: ''"
      }
    }
  }

  private def awaitRequest = {
    await(connector.send(TestData.nrsPayload, VersionTwo))
  }

//  private def returnResponseForRequest(eventualResponse: Future[NrSubmissionId]) = {
//    when(mockWsPost.POST(anyString, any[NrsPayload], any[SeqOfHeader])(
//      any[Writes[NrsPayload]], any[HttpReads[NrSubmissionId]](), any[HeaderCarrier](), any[ExecutionContext]))
//      .thenReturn(eventualResponse)
//  }
}
