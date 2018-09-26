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

package unit.schemas

import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.services.XmlValidationService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Elem, Node, SAXException}

class FileUploadResponseSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  protected val MockConfiguration = mock[Configuration]
  protected val MockXml = mock[Node]

  protected val propertyName: String = "xsd.locations.fileuploadresponse"
  protected val xsdLocations: Seq[String] = Seq("/api/conf/2.0/schemas/wco/fileupload/FileUploadResponse.xsd")

  def xmlValidationService: XmlValidationService = new XmlValidationService(MockConfiguration, schemaPropertyName = propertyName) {}

  override protected def beforeEach() {
    reset(MockConfiguration)
    when(MockConfiguration.getStringSeq(propertyName)).thenReturn(Some(xsdLocations))
    when(MockConfiguration.getInt("xml.max-errors")).thenReturn(None)
  }

  "File upload response" should {
    "be successfully validated if correct" in {
      val result: Unit = await(xmlValidationService.validate(ValidFileUploadResponseXML))

      result should be(())
    }

    "fail validation if is incorrect" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidFileUploadResponseXML))
      }

      caught.getMessage shouldBe "cvc-complex-type.2.4.b: The content of element 'file' is not complete. One of '{\"hmrc:fileupload\":reference, \"hmrc:fileupload\":uploadRequest}' is expected."

      Option(caught.getException) shouldBe None
    }

  }

  private val InvalidFileUploadResponseXML =
    <fileUploadResponse xmlns="hmrc:fileupload">
      <files>
        <file></file>
      </files>
    </fileUploadResponse>

  private val ValidFileUploadResponseXML: Elem =
    <fileUploadResponse xmlns="hmrc:fileupload">
      <files>
        <file>
          <reference>11370e18-6e24-453e-b45a-76d3e32ea33d</reference>
          <uploadRequest>
            <href>https://bucketName.s3.eu-west-2.amazonaws.com</href>
            <fields>
              <content-type>application/xml</content-type>
              <acl>private</acl>
              <key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
              <policy>xxxxxxxx==</policy>
              <X-Amz-algorithm>AWS4-HMAC-SHA256</X-Amz-algorithm>
              <X-Amz-credential>ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request</X-Amz-credential>
              <X-Amz-date>2018-02-09T12:35:45.297Z</X-Amz-date>
              <x-amz-meta-callback-url>https://myservice.com/callback</x-amz-meta-callback-url>
              <X-Amz-signature>xxxx</X-Amz-signature>
            </fields>
          </uploadRequest>
        </file>
        <file>
          <reference>11370e18-6e24-453e-b45a-76d3e32ea33d</reference>
          <uploadRequest>
            <href>https://bucketName.s3.eu-west-2.amazonaws.com</href>
            <fields>
              <content-type>application/xml</content-type>
              <acl>private</acl>
              <key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
              <policy>xxxxxxxx==</policy>
              <X-Amz-algorithm>AWS4-HMAC-SHA256</X-Amz-algorithm>
              <X-Amz-credential>ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request</X-Amz-credential>
              <X-Amz-date>2018-02-09T12:35:45.297Z</X-Amz-date>
              <x-amz-meta-callback-url>https://myservice.com/callback</x-amz-meta-callback-url>
              <X-Amz-signature>xxxx</X-Amz-signature>
            </fields>
          </uploadRequest>
        </file>
      </files>
    </fileUploadResponse>
}
