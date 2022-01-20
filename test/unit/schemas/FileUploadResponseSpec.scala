/*
 * Copyright 2022 HM Revenue & Customs
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

package unit.schemas

import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.services.XmlValidationService

import scala.xml.{Elem, Node, SAXException}

class FileUploadResponseSpec extends AnyWordSpecLike with MockitoSugar with BeforeAndAfterEach with Matchers{

  private implicit val ec = Helpers.stubControllerComponents().executionContext
  protected val MockConfiguration = mock[Configuration]
  protected val MockXml = mock[Node]

  protected val propertyName: String = "xsd.locations.fileuploadresponse"
  protected val xsdLocations: Seq[String] = Seq("/api/conf/2.0/schemas/wco/fileupload/FileUploadResponse.xsd")

  def xmlValidationService: XmlValidationService = new XmlValidationService(MockConfiguration, schemaPropertyName = propertyName) {}

  override protected def beforeEach() {
    reset(MockConfiguration)
    when(MockConfiguration.getOptional[Seq[String]](propertyName)).thenReturn(Some(xsdLocations))
    when(MockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)
  }

  "File upload response" should {
    "be successfully validated if correct" in {
      val result: Unit = (xmlValidationService.validate(ValidFileUploadResponseXML)).futureValue

      result should be(())
    }

    "fail validation if is incorrect" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidFileUploadResponseXML))
      }

      caught.getMessage shouldBe "cvc-complex-type.2.4.b: The content of element 'File' is not complete. One of '{\"hmrc:fileupload\":Reference, \"hmrc:fileupload\":UploadRequest}' is expected."

      Option(caught.getException) shouldBe None
    }

  }

  private val InvalidFileUploadResponseXML =
    <FileUploadResponse xmlns="hmrc:fileupload">
      <Files>
        <File></File>
      </Files>
    </FileUploadResponse>

  private val ValidFileUploadResponseXML: Elem =
    <FileUploadResponse xmlns="hmrc:fileupload">
      <Files>
        <File>
          <Reference>11370e18-6e24-453e-b45a-76d3e32ea33d</Reference>
          <UploadRequest>
            <Href>https://bucketName.s3.eu-west-2.amazonaws.com</Href>
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
            </Fields>
          </UploadRequest>
        </File>
        <File>
          <Reference>11370e18-6e24-453e-b45a-76d3e32ea33d</Reference>
          <UploadRequest>
            <Href>https://bucketName.s3.eu-west-2.amazonaws.com</Href>
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
            </Fields>
          </UploadRequest>
        </File>
      </Files>
    </FileUploadResponse>
}
