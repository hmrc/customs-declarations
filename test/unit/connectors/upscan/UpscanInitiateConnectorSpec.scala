/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.Writes
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers
import uk.gov.hmrc.customs.declaration.connectors.upscan.UpscanInitiateConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedFileUploadPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import util.UnitSpec
import util.TestData.{EmulatedServiceFailure, ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles, emulatedServiceFailure, fileUploadConfig}

import scala.concurrent.{ExecutionContext, Future}

class UpscanInitiateConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private implicit val ec = Helpers.stubControllerComponents().executionContext

  private val connector = new UpscanInitiateConnector(mockWsPost, mockLogger, mockDeclarationsConfigService)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpException = new NotFoundException("Emulated 404 response from a web call")
  private val tenThousand = 10000
  private val upscanInitiatePayloadV1WithNoRedirects = UpscanInitiatePayload("https://callbackurl.com", tenThousand, None, None)
  private val upscanInitiatePayloadV1WithSuccessRedirects = UpscanInitiatePayload("https://callbackurl.com", tenThousand, Some("https://success-redirect.com"), None)
  private val upscanInitiatePayloadV1WithErrorRedirects = UpscanInitiatePayload("https://callbackurl.com", tenThousand, None, Some("https://error-redirect.com"))
  private val upscanInitiatePayloadV2 = UpscanInitiatePayload("https://callbackurl.com", tenThousand, Some("https://success-redirect.com"), Some("https://error-redirect.com"))

  implicit val jsonRequest: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles

  override protected def beforeEach() {
    reset(mockWsPost, mockLogger)
    when(mockDeclarationsConfigService.fileUploadConfig).thenReturn(fileUploadConfig)
  }

  "UpscanInitiateConnector" can {

    "when making a successful request" should {

      "select V1 URL from config when no redirect values are present" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest(upscanInitiatePayloadV1WithNoRedirects)

        verify(mockWsPost).POST(ameq("upscan-initiate-v1.url"), any[UpscanInitiatePayload], any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "select V1 URL from config when success redirect and no error redirect values are present" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest(upscanInitiatePayloadV1WithSuccessRedirects)

        verify(mockWsPost).POST(ameq("upscan-initiate-v1.url"), any[UpscanInitiatePayload], any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "select V1 URL from config when error redirect and no success redirect values are present" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest(upscanInitiatePayloadV1WithErrorRedirects)

        verify(mockWsPost).POST(ameq("upscan-initiate-v1.url"), any[UpscanInitiatePayload], any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "select V2 URL from config when both redirect values are present" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest()

        verify(mockWsPost).POST(ameq("upscan-initiate-v2.url"), any[UpscanInitiatePayload], any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest()

        verify(mockWsPost).POST(anyString, ameq(upscanInitiatePayloadV2), any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when MDG call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(emulatedServiceFailure))

        val caught = intercept[EmulatedServiceFailure] {
          awaitRequest()
        }
        caught shouldBe emulatedServiceFailure
      }

      "wrap an underlying error when MDG call fails with an http exception" in {
        returnResponseForRequest(Future.failed(httpException))

        val caught = intercept[RuntimeException] {
          awaitRequest()
        }
        caught.getCause shouldBe httpException
      }
    }
  }

  private def awaitRequest(payload: UpscanInitiatePayload = upscanInitiatePayloadV2): UpscanInitiateResponsePayload = {
    await(connector.send(payload, VersionOne))
  }

  private def returnResponseForRequest(eventualResponse: Future[UpscanInitiateResponsePayload]) = {
    when(mockWsPost.POST(anyString, any[UpscanInitiatePayload], any[SeqOfHeader])(
      any[Writes[UpscanInitiatePayload]], any[HttpReads[UpscanInitiateResponsePayload]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
