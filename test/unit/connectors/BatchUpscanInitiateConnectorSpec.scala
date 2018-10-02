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

import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Writes
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.connectors.BatchUpscanInitiateConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.{EmulatedServiceFailure, emulatedServiceFailure, ValidatedBatchFileUploadPayloadRequestWithTwoFiles}

import scala.concurrent.{ExecutionContext, Future}

class BatchUpscanInitiateConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[HttpClient]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]

  private val connector = new BatchUpscanInitiateConnector(mockWsPost, mockLogger, mockServiceConfigProvider)

  private val v1Config = ServiceConfig("v1-url", Some("v1-bearer-token"), "v1-default")
  private val v2Config = ServiceConfig("v2-url", Some("v2-bearer-token"), "v2-default")

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val httpException = new NotFoundException("Emulated 404 response from a web call")
  private val upscanInitiatePayload = UpscanInitiatePayload("https://callbackurl.com")

  implicit val jsonRequest = ValidatedBatchFileUploadPayloadRequestWithTwoFiles

  override protected def beforeEach() {
    reset(mockWsPost, mockLogger, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("upscan-initiate")).thenReturn(v1Config)
    when(mockServiceConfigProvider.getConfig("v2.upscan-initiate")).thenReturn(v2Config)
  }

  "UpscanInitiateConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest

        verify(mockWsPost).POST(ameq(v2Config.url), any[UpscanInitiatePayload], any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass in the body" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        awaitRequest

        verify(mockWsPost).POST(anyString, ameq(upscanInitiatePayload), any[SeqOfHeader])(
          any[Writes[UpscanInitiatePayload]], any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(mock[UpscanInitiateResponsePayload]))

        await(connector.send(upscanInitiatePayload, VersionTwo))

        verify(mockServiceConfigProvider).getConfig("v2.upscan-initiate")
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

    "when configuration is absent" should {
      "throw an exception when no config is found for given api and version combination" in {
        when(mockServiceConfigProvider.getConfig("v2.upscan-initiate")).thenReturn(null)

        val caught = intercept[IllegalArgumentException] {
          awaitRequest
        }
        caught.getMessage shouldBe "config not found"
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
