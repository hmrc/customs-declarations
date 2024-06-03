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

import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Json, Writes}
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declaration.connectors.NrsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HttpClient, _}
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.RequestHeaders.OtherHaders
import util.TestData
import util.TestData.{fileUploadConfig, nrsConfigEnabled}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class NrsConnectorSpec extends AnyWordSpecLike with MockitoSugar with BeforeAndAfterEach with Eventually with Matchers{

  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private val mockWsPost = mock[HttpClient]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val connector = new NrsConnector(mockWsPost, mockLogger, mockDeclarationsConfigService)
  private implicit val hc: HeaderCarrier = HeaderCarrier()

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
    when(mockDeclarationsConfigService.fileUploadConfig).thenReturn(fileUploadConfig)
    when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)
  }

  "NrsConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(TestData.nrSubmissionId))

        awaitRequest

        verify(mockWsPost).POST(ameq(nrsConfigEnabled.nrsUrl), any[UpscanInitiatePayload], any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(TestData.nrSubmissionId))

        awaitRequest

        verify(mockWsPost).POST(anyString, ameq(TestData.nrsPayload), any[SeqOfHeader])(
          any[Writes[NrsPayload]], any[HttpReads[NrSubmissionId]](), any[HeaderCarrier](), any[ExecutionContext])
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

        val caught = intercept[UpstreamErrorResponse] {
          awaitRequest
        }
        caught shouldBe httpException
      }
    }

    "when Gov-Test-Scenario in the header" should {
      "overwrite value to default" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = OtherHaders.toSeq :+ ("Gov-Test-Scenario", "AMEND_DECLARATION"))
        val updatedHC = connector.overwriteGovTestScenarioHeaderValue(OtherHaders.toSeq)(hc)
        updatedHC.find(_._1.equals("Gov-Test-Scenario")).get._2 shouldBe "DEFAULT"
        updatedHC.size shouldBe 3
      }
    }

    "when Gov-Test-Scenario is not in the header" should {
      "not be added" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = OtherHaders.toSeq)
        val updatedHC = connector.overwriteGovTestScenarioHeaderValue(OtherHaders.toSeq)(hc)
        updatedHC.find(_._1.equals("Gov-Test-Scenario")) shouldBe None
        updatedHC.size shouldBe 2
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
