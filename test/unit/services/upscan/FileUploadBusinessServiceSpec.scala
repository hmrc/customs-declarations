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

package unit.services.upscan

import java.util.UUID

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{atLeastOnce, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{AnyContentAsJson, Result}
import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declaration.connectors.upscan.UpscanInitiateConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ValidatedFileUploadPayloadRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.model.upscan.FileUploadMetadata
import uk.gov.hmrc.customs.declaration.model.{UpscanInitiateResponsePayload, _}
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.upscan.FileUploadBusinessService
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, UuidService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._

import scala.concurrent.Future

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

    protected val xmlResponse: String =
      """<FileUploadResponse xmlns="hmrc:fileupload">
        |  <Files>
        |    <File>
        |      <Reference>31400000-8ce0-11bd-b23e-10b96e4ef00f</Reference>
        |      <UploadRequest>
        |        <Href>https://a.b.com</Href>
        |        <Fields>
        |          <Content-Type>application/xml; charset=utf-8</Content-Type>
        |          <x-amz-meta-callback-url>https://some-callback-url</x-amz-meta-callback-url>
        |          <x-amz-date>2019-03-05T11:56:34Z</x-amz-date>
        |          <x-amz-credential>ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request</x-amz-credential>
        |          <x-amz-meta-upscan-initiate-response>response</x-amz-meta-upscan-initiate-response>
        |          <x-amz-meta-upscan-initiate-received>received</x-amz-meta-upscan-initiate-received>
        |          <x-amz-meta-request-id>123</x-amz-meta-request-id>
        |          <x-amz-meta-original-filename>some-filename</x-amz-meta-original-filename>
        |          <x-amz-algorithm>AWS4-HMAC-SHA256</x-amz-algorithm>
        |          <key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
        |          <acl>private</acl>
        |          <x-amz-signature>xxxx</x-amz-signature>
        |          <x-amz-meta-session-id>789</x-amz-meta-session-id>
        |          <x-amz-meta-consuming-service>a-service-name</x-amz-meta-consuming-service>
        |          <policy>xxxxxxxx==</policy>
        |        </Fields>
        |      </UploadRequest>
        |    </File>
        |    <File>
        |      <Reference>32400000-8cf0-11bd-b23e-10b96e4ef00f</Reference>
        |      <UploadRequest>
        |        <Href>https://x.y.com</Href>
        |        <Fields>
        |          <Content-Type>application/xml; charset=utf-8</Content-Type>
        |          <x-amz-meta-callback-url>https://some-callback-url2</x-amz-meta-callback-url>
        |          <x-amz-date>2019-03-04T11:56:34Z</x-amz-date>
        |          <x-amz-credential>ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request</x-amz-credential>
        |          <x-amz-meta-upscan-initiate-response>response</x-amz-meta-upscan-initiate-response>
        |          <x-amz-meta-upscan-initiate-received>received</x-amz-meta-upscan-initiate-received>
        |          <x-amz-meta-request-id>123</x-amz-meta-request-id>
        |          <x-amz-meta-original-filename>some-filename</x-amz-meta-original-filename>
        |          <x-amz-algorithm>AWS4-HMAC-SHA256</x-amz-algorithm>
        |          <key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
        |          <acl>private</acl>
        |          <x-amz-signature>xxxx</x-amz-signature>
        |          <x-amz-meta-session-id>789</x-amz-meta-session-id>
        |          <x-amz-meta-consuming-service>a-service-name</x-amz-meta-consuming-service>
        |          <policy>xxxxxxxx==</policy>
        |        </Fields>
        |      </UploadRequest>
        |    </File>
        |  </Files>
        |</FileUploadResponse>""".stripMargin

    protected val xmlResponseWithEmptyOptionals: String =
      """<FileUploadResponse xmlns="hmrc:fileupload">
        |  <Files>
        |    <File>
        |      <Reference>31400000-8ce0-11bd-b23e-10b96e4ef00f</Reference>
        |      <UploadRequest>
        |        <Href>https://a.b.com</Href>
        |        <Fields>
        |          <Content-Type>application/xml; charset=utf-8</Content-Type>
        |          <x-amz-meta-callback-url>https://some-callback-url</x-amz-meta-callback-url>
        |          <x-amz-date>2019-03-05T11:56:34Z</x-amz-date>
        |          <x-amz-credential>ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request</x-amz-credential>
        |          <x-amz-meta-upscan-initiate-response>response</x-amz-meta-upscan-initiate-response>
        |          <x-amz-meta-upscan-initiate-received>received</x-amz-meta-upscan-initiate-received>
        |          <x-amz-meta-request-id>123</x-amz-meta-request-id>
        |          <x-amz-meta-original-filename>some-filename</x-amz-meta-original-filename>
        |          <x-amz-algorithm>AWS4-HMAC-SHA256</x-amz-algorithm>
        |          <key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
        |          <acl>private</acl>
        |          <x-amz-signature>xxxx</x-amz-signature>
        |          <x-amz-meta-session-id>789</x-amz-meta-session-id>
        |          <x-amz-meta-consuming-service>a-service-name</x-amz-meta-consuming-service>
        |          <policy>xxxxxxxx==</policy>
        |        </Fields>
        |      </UploadRequest>
        |    </File>
        |    <File>
        |      <Reference>32400000-8cf0-11bd-b23e-10b96e4ef00f</Reference>
        |      <UploadRequest>
        |        <Href>https://x.y.com</Href>
        |        <Fields>
        |          <acl>some-acl</acl>
        |        </Fields>
        |      </UploadRequest>
        |    </File>
        |  </Files>
        |</FileUploadResponse>""".stripMargin

    implicit val jsonRequest: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles

    val upscanInitiatePayload = UpscanInitiatePayload("http://file-upload-upscan-callback.url/uploaded-file-upscan-notifications/clientSubscriptionId/327d9145-4965-4d28-a2c5-39dedee50334")
    val upscanInitiateResponseFields1: Map[String, String] = Map(("Content-Type","application/xml; charset=utf-8"), ("acl","private"),
      ("key","xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"), ("policy","xxxxxxxx=="), ("x-amz-algorithm","AWS4-HMAC-SHA256"),
      ("x-amz-credential","ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request"), ("x-amz-date","2019-03-05T11:56:34Z"),
      ("x-amz-meta-callback-url","https://some-callback-url"), ("x-amz-signature","xxxx"), ("x-amz-meta-upscan-initiate-response", "response"),
      ("x-amz-meta-upscan-initiate-received", "received"), ("x-amz-meta-request-id", "123"), ("x-amz-meta-original-filename", "some-filename"),
      ("x-amz-meta-session-id", "789"), ("x-amz-meta-consuming-service", "a-service-name"))
    val upscanInitiateResponseFields2: Map[String, String] = Map(("Content-Type","application/xml; charset=utf-8"), ("acl","private"),
      ("key","xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"), ("policy","xxxxxxxx=="), ("x-amz-algorithm","AWS4-HMAC-SHA256"),
      ("x-amz-credential","ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request"), ("x-amz-date","2019-03-04T11:56:34Z"),
      ("x-amz-meta-callback-url","https://some-callback-url2"), ("x-amz-signature","xxxx"), ("x-amz-meta-upscan-initiate-response", "response"),
      ("x-amz-meta-upscan-initiate-received", "received"), ("x-amz-meta-request-id", "123"), ("x-amz-meta-original-filename", "some-filename"),
      ("x-amz-meta-session-id", "789"), ("x-amz-meta-consuming-service", "a-service-name"))
    val upscanInitiateResponsePayload1 = UpscanInitiateResponsePayload(FileReferenceOne.value.toString, UpscanInitiateUploadRequest("https://a.b.com", upscanInitiateResponseFields1))
    val upscanInitiateResponsePayload2 = UpscanInitiateResponsePayload(FileReferenceTwo.value.toString, UpscanInitiateUploadRequest("https://x.y.com", upscanInitiateResponseFields2))
    val upscanInitiateResponsePayload3 = UpscanInitiateResponsePayload(FileReferenceTwo.value.toString, UpscanInitiateUploadRequest("https://x.y.com", Map(("Content-Type", "   "), ("new-field", "   "), ("acl", "some-acl"))))

    protected def send(vupr: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = jsonRequest, hc: HeaderCarrier = headerCarrier): Either[Result, String] = {
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

    "send payload to connector for non-CSP with optional fields" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]])).thenReturn(upscanInitiateResponsePayload1, upscanInitiateResponsePayload3)

      val result = send().right.get

      result shouldBe xmlResponseWithEmptyOptionals
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
