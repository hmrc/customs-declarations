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

package unit.services

import java.util.UUID

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, UpscanInitiateConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ValidatedPayloadRequest, ValidatedUploadPayloadRequest}
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, _}
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, FileUploadBusinessService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.TestData._

import scala.concurrent.Future
import scala.xml.NodeSeq

class FileUploadBusinessServiceSpec extends UnitSpec with MockitoSugar {
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

  trait SetUp {
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockUpscanInitiateConnector: UpscanInitiateConnector = mock[UpscanInitiateConnector]
    protected val mockUpscanInitiateResponsePayload: UpscanInitiateResponsePayload = mock[UpscanInitiateResponsePayload]
    protected val mockBatchFileUploadConfig = mock[BatchFileUploadConfig]
    protected val mockConfiguration = mock[DeclarationsConfigService]

    protected lazy val service: FileUploadBusinessService = new FileUploadBusinessService(mockLogger, mockApiSubscriptionFieldsConnector, mockUpscanInitiateConnector, mockConfiguration)

    private implicit val jsonRequest = ValidatedUploadPayloadRequest(
      ConversationId(UUID.randomUUID()),
      GoogleAnalyticsValues.Fileupload,
      EventStart,
      VersionTwo,
      ClientId("ABC"),
      NonCsp(Eori("123"), None),
      NodeSeq.Empty,
      FakeRequest().withJsonBody(Json.obj("fake" -> "request")),
      DeclarationId("decId123"),
      DocumentationType("docType456")
    )


    val upscanInitiatePayload = UpscanInitiatePayload(s"http://upscan-callback.url/uploaded-file-upscan-notifications/decId/decId123/eori/123/documentationType/docType456/clientSubscriptionId/$clientSubscriptionIdString")

    protected def send(vupr: ValidatedUploadPayloadRequest[AnyContentAsJson] = jsonRequest, hc: HeaderCarrier = headerCarrier): Either[Result, UpscanInitiateResponsePayload] = {
      await(service.send(vupr, hc))
    }
    when(mockBatchFileUploadConfig.upscanCallbackUrl).thenReturn("http://upscan-callback.url")
    when(mockConfiguration.batchFileUploadConfig).thenReturn(mockBatchFileUploadConfig)
    when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedUploadPayloadRequest[_]])).thenReturn(mockUpscanInitiateResponsePayload)
  }

  "BusinessService" should {

    "send payload to connector" in new SetUp() {

      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedUploadPayloadRequest[_]])).thenReturn(mockUpscanInitiateResponsePayload)

      val result: Either[Result, UpscanInitiateResponsePayload] = send()

      result shouldBe Right(mockUpscanInitiateResponsePayload)
      verify(mockUpscanInitiateConnector).send(meq(upscanInitiatePayload), any[ApiVersion])(any[ValidatedUploadPayloadRequest[_]])
    }

    "return 500 error response when subscription field lookup fails" in new SetUp() {

      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(new Exception))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedUploadPayloadRequest[_]])).thenReturn(mockUpscanInitiateResponsePayload)

      val result: Result = send().left.get

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 500 error response when upscan initiate call fails" in new SetUp() {

      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedUploadPayloadRequest[_]])).thenReturn(Future.failed(new Exception))

      val result: Result = send().left.get

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }
}

