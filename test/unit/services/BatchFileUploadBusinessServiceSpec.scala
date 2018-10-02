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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{atLeastOnce, times, verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, BatchUpscanInitiateConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ValidatedBatchFileUploadPayloadRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.model.{UpscanInitiateResponsePayload, _}
import uk.gov.hmrc.customs.declaration.repo.BatchFileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.{BatchFileUploadBusinessService, DeclarationsConfigService, UuidService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData

import scala.concurrent.Future
import scala.xml.NodeSeq

class BatchFileUploadBusinessServiceSpec extends UnitSpec with MockitoSugar {

  private val headerCarrier: HeaderCarrier = HeaderCarrier()

  trait SetUp {
    protected val mockBatchFileUploadMetadataRepo: BatchFileUploadMetadataRepo = mock[BatchFileUploadMetadataRepo]
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockBatchUpscanInitiateConnector: BatchUpscanInitiateConnector = mock[BatchUpscanInitiateConnector]
    protected val mockUpscanInitiateResponsePayload: UpscanInitiateResponsePayload = mock[UpscanInitiateResponsePayload]
    protected val mockBatchFileUploadConfig = mock[BatchFileUploadConfig]
    protected val mockConfiguration = mock[DeclarationsConfigService]
    protected val mockUuidService = mock[UuidService]

    protected lazy val service = new BatchFileUploadBusinessService(mockBatchUpscanInitiateConnector,
      mockBatchFileUploadMetadataRepo, mockUuidService, mockLogger, mockApiSubscriptionFieldsConnector, mockConfiguration)

    val xmlResponse = <FileUploadResponse>
      <Files>
        <File>
          <reference>31400000-8ce0-11bd-b23e-10b96e4ef00f</reference>
          <uploadRequest>
            <href>https://a.b.com</href>
            <fields>
              <label1>value1</label1><label2>value2</label2>
            </fields>
          </uploadRequest>
        </File><File>
          <reference>32400000-8cf0-11bd-b23e-10b96e4ef00f</reference>
          <uploadRequest>
            <href>https://x.y.com</href>
            <fields>
              <labelx>valuey</labelx>
            </fields>
          </uploadRequest>
        </File>
      </Files>
    </FileUploadResponse>

    implicit val jsonRequest = TestData.ValidatedBatchFileUploadPayloadRequestWithTwoFiles

    val upscanInitiatePayload = UpscanInitiatePayload("http://upscan-callback.url/uploaded-file-upscan-notifications/decId/decId123/eori/123/documentationType/docType1/clientSubscriptionId/327d9145-4965-4d28-a2c5-39dedee50334")
    val upscanInitiateResponsePayload1 = UpscanInitiateResponsePayload(TestData.FileReferenceOne.value.toString, UpscanInitiateUploadRequest("https://a.b.com", Map(("label1","value1"), ("label2","value2"))))
    val upscanInitiateResponsePayload2 = UpscanInitiateResponsePayload(TestData.FileReferenceTwo.value.toString, UpscanInitiateUploadRequest("https://x.y.com", Map(("labelx","valuey"))))

    protected def send(vupr: ValidatedBatchFileUploadPayloadRequest[AnyContentAsJson] = jsonRequest, hc: HeaderCarrier = headerCarrier): Either[Result, NodeSeq] = {
      await(service.send(vupr, hc))
    }

    when(mockBatchFileUploadConfig.upscanCallbackUrl).thenReturn("http://upscan-callback.url")
    when(mockConfiguration.batchFileUploadConfig).thenReturn(mockBatchFileUploadConfig)
    when(mockUuidService.uuid()).thenReturn(any[UUID])
    when(mockBatchFileUploadMetadataRepo.create(any[BatchFileUploadMetadata])).thenReturn(Future.successful(true))
  }

  "BatchFileUploadBusinessService" should {
    "send payload to connector" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockBatchUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedBatchFileUploadPayloadRequest[_]])).thenReturn(upscanInitiateResponsePayload1, upscanInitiateResponsePayload2)

      val result = send().right.get

      result shouldBe xmlResponse
      verify(mockBatchUpscanInitiateConnector, atLeastOnce()).send(meq(upscanInitiatePayload), meq(VersionTwo))(meq(jsonRequest))
    }

    "fail fast when sending payloads to connector" in new SetUp() {

      val successfulConnectorSend = Future.successful(upscanInitiateResponsePayload1)
      val failedConnectorSend = Future.failed(TestData.emulatedServiceFailure)
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockBatchUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedBatchFileUploadPayloadRequest[_]])).thenReturn(successfulConnectorSend, successfulConnectorSend, failedConnectorSend, successfulConnectorSend)

      val result = send(TestData.ValidatedBatchFileUploadPayloadRequestWithFourFiles).left.get

      verify(mockBatchUpscanInitiateConnector, times(3)).send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedBatchFileUploadPayloadRequest[_]])
    }

    "return 500 error response when subscription field lookup fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(TestData.emulatedServiceFailure))
      when(mockBatchUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedBatchFileUploadPayloadRequest[_]])).thenReturn(mockUpscanInitiateResponsePayload)

      val result = send().left.get

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 500 error response when upscan initiate call fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockBatchUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedBatchFileUploadPayloadRequest[_]])).thenReturn(Future.failed(TestData.emulatedServiceFailure))

      val result = send().left.get

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}
