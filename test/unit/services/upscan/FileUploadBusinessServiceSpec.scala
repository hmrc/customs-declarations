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

package unit.services.upscan

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{atLeastOnce, times, verify, when}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declaration.connectors.upscan.UpscanInitiateConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ValidatedFileUploadPayloadRequest, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.model.upscan.FileUploadMetadata
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.upscan.FileUploadBusinessService
import uk.gov.hmrc.customs.declaration.services.{DateTimeService, DeclarationsConfigService, UuidService}
import uk.gov.hmrc.http.HeaderCarrier
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}

class FileUploadBusinessServiceSpec extends AnyWordSpec with MockitoSugar with GuiceOneAppPerSuite with Matchers with EitherValues{

  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private val headerCarrier: HeaderCarrier = HeaderCarrier()

  trait SetUp {
    protected val mockFileUploadMetadataRepo: FileUploadMetadataRepo = mock[FileUploadMetadataRepo]
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockApiSubscriptionFieldsConnector: ApiSubscriptionFieldsConnector = mock[ApiSubscriptionFieldsConnector]
    protected val mockUpscanInitiateConnector: UpscanInitiateConnector = mock[UpscanInitiateConnector]
    protected val mockUpscanInitiateResponsePayload: UpscanInitiateResponsePayload = mock[UpscanInitiateResponsePayload]
    protected val mockFileUploadConfig: FileUploadConfig = mock[FileUploadConfig]
    protected val mockConfiguration: DeclarationsConfigService = mock[DeclarationsConfigService]
    protected val mockUuidService: UuidService = mock[UuidService]
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]

    protected lazy val service = new FileUploadBusinessService(mockUpscanInitiateConnector,
      mockFileUploadMetadataRepo, mockUuidService, mockDateTimeService, mockLogger, mockApiSubscriptionFieldsConnector, mockConfiguration)

    protected val xmlResponse: Elem =
      <FileUploadResponse xmlns="hmrc:fileupload">
        <Files>
          <File>
            <Reference>31400000-8ce0-11bd-b23e-10b96e4ef00f</Reference>
            <UploadRequest>
              <Href>https://a.b.com</Href>
              <Fields>
                <Content-Type>application/xml; charset=utf-8</Content-Type>
                <x-amz-meta-callback-url>https://some-callback-url</x-amz-meta-callback-url>
                <x-amz-date>2019-03-05T11:56:34Z</x-amz-date>
                <x-amz-credential>ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request</x-amz-credential>
                <x-amz-meta-upscan-initiate-response>response</x-amz-meta-upscan-initiate-response>
                <x-amz-meta-upscan-initiate-received>received</x-amz-meta-upscan-initiate-received>
                <x-amz-meta-request-id>123</x-amz-meta-request-id>
                <x-amz-meta-original-filename>some-filename</x-amz-meta-original-filename>
                <x-amz-algorithm>AWS4-HMAC-SHA256</x-amz-algorithm>
                <key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
                <acl>private</acl>
                <x-amz-signature>xxxx</x-amz-signature>
                <x-amz-meta-session-id>789</x-amz-meta-session-id>
                <x-amz-meta-consuming-service>a-service-name</x-amz-meta-consuming-service>
                <policy>xxxxxxxx==</policy>
                <success_action_redirect>https://success-redirect.com</success_action_redirect>
                <error_action_redirect>https://error-redirect.com</error_action_redirect>
              </Fields>
            </UploadRequest>
          </File>
          <File>
            <Reference>32400000-8cf0-11bd-b23e-10b96e4ef00f</Reference>
            <UploadRequest>
              <Href>https://x.y.com</Href>
              <Fields>
                <Content-Type>application/xml; charset=utf-8</Content-Type>
                <x-amz-meta-callback-url>https://some-callback-url2</x-amz-meta-callback-url>
                <x-amz-date>2019-03-04T11:56:34Z</x-amz-date>
                <x-amz-credential>ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request</x-amz-credential>
                <x-amz-meta-upscan-initiate-response>response</x-amz-meta-upscan-initiate-response>
                <x-amz-meta-upscan-initiate-received>received</x-amz-meta-upscan-initiate-received>
                <x-amz-meta-request-id>123</x-amz-meta-request-id>
                <x-amz-meta-original-filename>some-filename</x-amz-meta-original-filename>
                <x-amz-algorithm>AWS4-HMAC-SHA256</x-amz-algorithm>
                <key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
                <acl>private</acl>
                <x-amz-signature>xxxx</x-amz-signature>
                <x-amz-meta-session-id>789</x-amz-meta-session-id>
                <x-amz-meta-consuming-service>a-service-name</x-amz-meta-consuming-service>
                <policy>xxxxxxxx==</policy>
                <success_action_redirect>https://success-redirect.com</success_action_redirect>
                <error_action_redirect>https://error-redirect.com</error_action_redirect>
              </Fields>
            </UploadRequest>
          </File>
        </Files>
      </FileUploadResponse>

    protected val xmlResponseWithEmptyOptionals: Elem =
      <FileUploadResponse xmlns="hmrc:fileupload">
        <Files>
          <File>
            <Reference>31400000-8ce0-11bd-b23e-10b96e4ef00f</Reference>
            <UploadRequest>
              <Href>https://a.b.com</Href>
              <Fields>
                <Content-Type>application/xml; charset=utf-8</Content-Type>
                <x-amz-meta-callback-url>https://some-callback-url</x-amz-meta-callback-url>
                <x-amz-date>2019-03-05T11:56:34Z</x-amz-date>
                <x-amz-credential>ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request</x-amz-credential>
                <x-amz-meta-upscan-initiate-response>response</x-amz-meta-upscan-initiate-response>
                <x-amz-meta-upscan-initiate-received>received</x-amz-meta-upscan-initiate-received>
                <x-amz-meta-request-id>123</x-amz-meta-request-id>
                <x-amz-meta-original-filename>some-filename</x-amz-meta-original-filename>
                <x-amz-algorithm>AWS4-HMAC-SHA256</x-amz-algorithm>
                <key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
                <acl>private</acl>
                <x-amz-signature>xxxx</x-amz-signature>
                <x-amz-meta-session-id>789</x-amz-meta-session-id>
                <x-amz-meta-consuming-service>a-service-name</x-amz-meta-consuming-service>
                <policy>xxxxxxxx==</policy>
                <success_action_redirect>https://success-redirect.com</success_action_redirect>
                <error_action_redirect>https://error-redirect.com</error_action_redirect>
              </Fields>
            </UploadRequest>
          </File>
          <File>
            <Reference>32400000-8cf0-11bd-b23e-10b96e4ef00f</Reference>
            <UploadRequest>
              <Href>https://x.y.com</Href>
              <Fields>
                <Content-Type>   </Content-Type>
                <acl>some-acl</acl>
                <success_action_redirect>https://success-redirect.com</success_action_redirect>
                <error_action_redirect>https://error-redirect.com</error_action_redirect>
              </Fields>
            </UploadRequest>
          </File>
        </Files>
      </FileUploadResponse>

    implicit val jsonRequest: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = ValidatedFileUploadPayloadRequestForNonCspWithTwoFiles

    val upscanInitiatePayload: UpscanInitiatePayload = UpscanInitiatePayload("http://file-upload-upscan-callback.url/uploaded-file-upscan-notifications/clientSubscriptionId/327d9145-4965-4d28-a2c5-39dedee50334", 10000, Some("https://success-redirect.com"), Some("https://error-redirect.com"))
    val upscanInitiateResponseFields1: Map[String, String] = Map(("Content-Type","application/xml; charset=utf-8"), ("acl","private"),
      ("key","xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"), ("policy","xxxxxxxx=="), ("x-amz-algorithm","AWS4-HMAC-SHA256"),
      ("x-amz-credential","ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request"), ("x-amz-date","2019-03-05T11:56:34Z"),
      ("x-amz-meta-callback-url","https://some-callback-url"), ("x-amz-signature","xxxx"), ("x-amz-meta-upscan-initiate-response", "response"),
      ("x-amz-meta-upscan-initiate-received", "received"), ("x-amz-meta-request-id", "123"), ("x-amz-meta-original-filename", "some-filename"),
      ("x-amz-meta-session-id", "789"), ("x-amz-meta-consuming-service", "a-service-name"), ("success_action_redirect", "https://success-redirect.com"),
      ("error_action_redirect", "https://error-redirect.com"))
    val upscanInitiateResponseFields2: Map[String, String] = Map(("Content-Type","application/xml; charset=utf-8"), ("acl","private"),
      ("key","xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"), ("policy","xxxxxxxx=="), ("x-amz-algorithm","AWS4-HMAC-SHA256"),
      ("x-amz-credential","ASIAxxxxxxxxx/20190304/eu-west-2/s3/aws4_request"), ("x-amz-date","2019-03-04T11:56:34Z"),
      ("x-amz-meta-callback-url","https://some-callback-url2"), ("x-amz-signature","xxxx"), ("x-amz-meta-upscan-initiate-response", "response"),
      ("x-amz-meta-upscan-initiate-received", "received"), ("x-amz-meta-request-id", "123"), ("x-amz-meta-original-filename", "some-filename"),
      ("x-amz-meta-session-id", "789"), ("x-amz-meta-consuming-service", "a-service-name"), ("success_action_redirect", "https://success-redirect.com"),
      ("error_action_redirect", "https://error-redirect.com"))
    val upscanInitiateResponsePayload1: UpscanInitiateResponsePayload = UpscanInitiateResponsePayload(FileReferenceOne.value.toString, UpscanInitiateUploadRequest("https://a.b.com", upscanInitiateResponseFields1))
    val upscanInitiateResponsePayload2: UpscanInitiateResponsePayload = UpscanInitiateResponsePayload(FileReferenceTwo.value.toString, UpscanInitiateUploadRequest("https://x.y.com", upscanInitiateResponseFields2))
    val upscanInitiateResponsePayload3: UpscanInitiateResponsePayload = UpscanInitiateResponsePayload(FileReferenceTwo.value.toString, UpscanInitiateUploadRequest("https://x.y.com", Map(("Content-Type", "   "), ("new-field", "   "), ("acl", "some-acl"),
      ("success_action_redirect", "https://success-redirect.com"), ("error_action_redirect", "https://error-redirect.com"))))

    protected def send(vupr: ValidatedFileUploadPayloadRequest[AnyContentAsJson] = jsonRequest, hc: HeaderCarrier = headerCarrier): Either[Result, NodeSeq] = {
      (service.send(vupr, hc)).futureValue
    }

    when(mockFileUploadConfig.fileUploadCallbackUrl).thenReturn("http://file-upload-upscan-callback.url")
    when(mockFileUploadConfig.upscanInitiateMaximumFileSize).thenReturn(10000)
    when(mockConfiguration.fileUploadConfig).thenReturn(mockFileUploadConfig)
    when(mockUuidService.uuid()).thenReturn(any[UUID])
    when(mockFileUploadMetadataRepo.create(any[FileUploadMetadata])).thenReturn(Future.successful(true))
  }

  "FileUploadBusinessService" should {
    "send payload to connector for non-CSP" in new SetUp() {
      val successfulConnectorSend: Future[UpscanInitiateResponsePayload] = Future.successful(upscanInitiateResponsePayload1)
      val successfulConnectorSend2: Future[UpscanInitiateResponsePayload] = Future.successful(upscanInitiateResponsePayload2)
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]], any[HeaderCarrier]())).thenReturn(successfulConnectorSend,successfulConnectorSend2)

      val result: NodeSeq = send().toOption.get

      result shouldBe xmlResponse
      verify(mockUpscanInitiateConnector, atLeastOnce()).send(meq(upscanInitiatePayload), meq(VersionTwo))(meq(jsonRequest),  meq(headerCarrier))
    }

    "send payload to connector for non-CSP with optional fields" in new SetUp() {
      val successfulConnectorSend: Future[UpscanInitiateResponsePayload] = Future.successful(upscanInitiateResponsePayload1)
      val successfulConnectorSend2: Future[UpscanInitiateResponsePayload] = Future.successful(upscanInitiateResponsePayload3)
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]], any[HeaderCarrier])).thenReturn(successfulConnectorSend,successfulConnectorSend2)

      val result: NodeSeq = send().toOption.get

      result shouldBe xmlResponseWithEmptyOptionals
      verify(mockUpscanInitiateConnector, atLeastOnce()).send(meq(upscanInitiatePayload), meq(VersionTwo))(meq(jsonRequest), meq(headerCarrier))
    }

    "send payload to connector for CSP" in new SetUp() {
      val successfulConnectorSend: Future[UpscanInitiateResponsePayload] = Future.successful(upscanInitiateResponsePayload1)
      val successfulConnectorSend2: Future[UpscanInitiateResponsePayload] = Future.successful(upscanInitiateResponsePayload2)
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]], any[HeaderCarrier])).thenReturn(successfulConnectorSend,successfulConnectorSend2)

      val result: NodeSeq = send(ValidatedFileUploadPayloadRequestForCspWithTwoFiles).toOption.get

      result shouldBe xmlResponse
      verify(mockUpscanInitiateConnector, atLeastOnce()).send(meq(upscanInitiatePayload), meq(VersionTwo))(meq(ValidatedFileUploadPayloadRequestForCspWithTwoFiles), meq(headerCarrier))
    }

    "fail fast when sending payloads to connector" in new SetUp() {

      val successfulConnectorSend: Future[UpscanInitiateResponsePayload] = Future.successful(upscanInitiateResponsePayload1)
      val failedConnectorSend: Future[Nothing] = Future.failed(emulatedServiceFailure)
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]], any[HeaderCarrier])).thenReturn(successfulConnectorSend, successfulConnectorSend, failedConnectorSend, successfulConnectorSend)

      val result: Future[Result] = Future.successful(send(ValidatedFileUploadPayloadRequestWithFourFiles).left.value)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR

      verify(mockUpscanInitiateConnector, times(3)).send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]], any[HeaderCarrier])
    }

    "return 500 error response when subscription field lookup fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(mockUpscanInitiateResponsePayload))

      val result: Future[Result] = Future.successful(send().left.value)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 500 error response when upscan initiate call fails" in new SetUp() {
      when(mockApiSubscriptionFieldsConnector.getSubscriptionFields(any[ApiSubscriptionKey])(any[ValidatedPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.successful(apiSubscriptionFieldsResponse))
      when(mockUpscanInitiateConnector.send(any[UpscanInitiatePayload], any[ApiVersion])(any[ValidatedFileUploadPayloadRequest[_]], any[HeaderCarrier])).thenReturn(Future.failed(emulatedServiceFailure))

      val result: Future[Result] = Future.successful(send().left.value)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}
