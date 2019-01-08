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

package unit.services

import java.util.UUID

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{atLeastOnce, times, verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.customs.declaration.connectors.{ApiSubscriptionFieldsConnector, UpscanInitiateConnector}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ValidatedFileUploadPayloadRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.model.{UpscanInitiateResponsePayload, _}
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.{FileUploadBusinessService, DeclarationsConfigService, UuidService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._

import scala.concurrent.Future
import scala.xml.NodeSeq

class FileUploadBusinessServiceSpec extends UnitSpec with MockitoSugar {

  private val headerCarrier: HeaderCarrier = HeaderCarrier()

  trait SetUp {
    protected val mockFileUploadMetadataRepo: FileUploadMetadataRepo = mock[FileUploadMetadataRepo]
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockUpscanInitiateConnector: UpscanInitiateConnector = mock[UpscanInitiateConnector]
    protected val mockUpscanInitiateResponsePayload: UpscanInitiateResponsePayload = mock[UpscanInitiateResponsePayload]
    protected val mockFileUploadConfig = mock[FileUploadConfig]
    protected val mockConfiguration = mock[DeclarationsConfigService]
    protected val mockUuidService = mock[UuidService]

    protected lazy val service = new FileUploadBusinessService(mockUpscanInitiateConnector,
      mockFileUploadMetadataRepo, mockUuidService, mockLogger, mockApiSubscriptionFieldsConnector, mockConfiguration)

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

    implicit val jsonRequest = ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles

    val upscanInitiatePayload = UpscanInitiatePayload("http://file-upload-upscan-callback.url/uploaded-file-upscan-notifications/clientSubscriptionId/327d9145-4965-4d28-a2c5-39dedee50334")
    val upscanInitiateResponsePayload1 = UpscanInitiateResponsePayload(FileReferenceOne.value.toString, UpscanInitiateUploadRequest("https://a.b.com", Map(("label1","value1"), ("label2","value2"))))
    val upscanInitiateResponsePayload2 = UpscanInitiateResponsePayload(FileReferenceTwo.value.toString, UpscanInitiateUploadRequest("https://x.y.com", Map(("labelx","valuey"))))

    protected def send(vupr: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = jsonRequest, hc: HeaderCarrier = headerCarrier): Either[Result, NodeSeq] = {
      await(service.send(vupr, hc))
    }

    when(mockFileUploadConfig.fileUploadCallbackUrl).thenReturn("http://file-upload-upscan-callback.url")
    when(mockConfiguration.fileUploadConfig).thenReturn(mockFileUploadConfig)
    when(mockUuidService.uuid()).thenReturn(any[UUID])
    when(mockFileUploadMetadataRepo.create(any[FileUploadMetadata])).thenReturn(Future.successful(true))
  }

  "FileUploadBusinessService" should {
    "send payload to connector for non-CSP" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]])).thenReturn(upscanInitiateResponsePayload1, upscanInitiateResponsePayload2)

      val result = send().right.get

      result shouldBe xmlResponse
      verify(mockUpscanInitiateConnector, atLeastOnce()).send(meq(upscanInitiatePayload), meq(VersionTwo))(meq(jsonRequest))
    }

    "send payload to connector for CSP" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]])).thenReturn(upscanInitiateResponsePayload1, upscanInitiateResponsePayload2)

      val result = send(ValidatedFileUploadPayloadRequestForCspWithTwoFiles).right.get

      result shouldBe xmlResponse
      verify(mockUpscanInitiateConnector, atLeastOnce()).send(meq(upscanInitiatePayload), meq(VersionTwo))(meq(ValidatedFileUploadPayloadRequestForCspWithTwoFiles))
    }

    "fail fast when sending payloads to connector" in new SetUp() {

      val successfulConnectorSend = Future.successful(upscanInitiateResponsePayload1)
      val failedConnectorSend = Future.failed(emulatedServiceFailure)
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]])).thenReturn(successfulConnectorSend, successfulConnectorSend, failedConnectorSend, successfulConnectorSend)

      val result = send(ValidatedFileUploadPayloadRequestWithFourFiles).left.get
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR

      verify(mockUpscanInitiateConnector, times(3)).send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]])
    }

    "return 500 error response when subscription field lookup fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]])).thenReturn(mockUpscanInitiateResponsePayload)

      val result = send().left.get

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 500 error response when upscan initiate call fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]])).thenReturn(Future.failed(emulatedServiceFailure))

      val result = send().left.get

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}
