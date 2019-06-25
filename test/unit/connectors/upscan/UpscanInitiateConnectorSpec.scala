/*
 * Copyright 2019 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Writes
import uk.gov.hmrc.customs.declaration.connectors.upscan.UpscanInitiateConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.{EmulatedServiceFailure, ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles, emulatedServiceFailure, fileUploadConfig}

import scala.concurrent.{ExecutionContext, Future}

class UpscanInitiateConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]

  private val connector = new UpscanInitiateConnector(mockWsPost, mockLogger, mockDeclarationsConfigService)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpException = new NotFoundException("Emulated 404 response from a web call")
  private val upscanInitiatePayload = UpscanInitiatePayload("https://callbackurl.com", 10000, Some("https://success-redirect.com"), Some("https://error-redirect.com"))

  implicit val jsonRequest = ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles

  override protected def beforeEach() {
    reset(mockWsPost, mockLogger)
    when(mockDeclarationsConfigService.fileUploadConfig).thenReturn(fileUploadConfig)
  }

  "UpscanInitiateConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest

        verify(mockWsPost).POST(ameq("upscan-initiate.url"), any[UpscanInitiatePayload], any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest

        verify(mockWsPost).POST(anyString, ameq(upscanInitiatePayload), any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when MDG call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(emulatedServiceFailure))

        val caught = intercept[EmulatedServiceFailure] {
          awaitRequest
        }
        caught shouldBe emulatedServiceFailure
      }

      "wrap an underlying error when MDG call fails with an http exception" in {
        returnResponseForRequest(Future.failed(httpException))

        val caught = intercept[RuntimeException] {
          awaitRequest
        }
        caught.getCause shouldBe httpException
      }
    }
  }

  private def awaitRequest = {
    await(connector.send(upscanInitiatePayload, VersionTwo))
  }

  private def returnResponseForRequest(eventualResponse: Future[UpscanInitiateResponsePayload]) = {
    when(mockWsPost.POST(anyString, any[UpscanInitiatePayload], any[SeqOfHeader])(
      any[Writes[UpscanInitiatePayload]], any[HttpReads[UpscanInitiateResponsePayload]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
