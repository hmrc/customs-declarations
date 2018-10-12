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
import play.api.libs.json.{Json, Writes}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.connectors.NrsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData
import util.TestData.nrsConfigEnabled

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class NrsConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val connector = new NrsConnector(mockWsPost, mockLogger, mockServiceConfigProvider, mockDeclarationsConfigService)

  private val v1Config = ServiceConfig("v1-url", None, "v1-default")
  private val v2Config = ServiceConfig("v2-url", None, "v2-default")

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private implicit val jsonRequest: ValidatedPayloadRequest[AnyContentAsJson] =  ValidatedPayloadRequest(
    ConversationId(UUID.randomUUID()),
    GoogleAnalyticsValues.Submit,
    VersionTwo,
    ClientId("ABC"),
    NonCsp(Eori("123"), Some(TestData.nrsRetrievalValues)),
    NodeSeq.Empty,
    FakeRequest().withJsonBody(Json.obj("fake" -> "request"))
  )

  private val httpException = new NotFoundException("Emulated 404 response from a web call")

  override protected def beforeEach() {
    reset(mockWsPost, mockLogger, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("nrs-service")).thenReturn(v1Config)
    when(mockServiceConfigProvider.getConfig("v2.nrs-service")).thenReturn(v2Config)
    when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
  }

  "NrsConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(TestData.nrSubmissionId))

        awaitRequest

        verify(mockWsPost).POST(ameq(v2Config.url), any[UpscanInitiatePayload], any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(TestData.nrSubmissionId))

        awaitRequest

        verify(mockWsPost).POST(anyString, ameq(TestData.nrsPayload), any[SeqOfHeader])(
          any[Writes[NrsPayload]], any[HttpReads[NrSubmissionId]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(TestData.nrSubmissionId))

        await(connector.send(TestData.nrsPayload, VersionTwo))

        verify(mockServiceConfigProvider).getConfig("v2.nrs-service")
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
        when(mockServiceConfigProvider.getConfig("v2.nrs-service")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          awaitRequest
        }
        caught.getMessage shouldBe "config not found"
      }
    }
  }

  private def awaitRequest = {
    await(connector.send(TestData.nrsPayload, VersionTwo))
  }

  private def returnResponseForRequest(eventualResponse: Future[NrSubmissionId]) = {
    when(mockWsPost.POST(anyString, any[NrsPayload], any[SeqOfHeader])(
      any[Writes[NrsPayload]], any[HttpReads[NrSubmissionId]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
