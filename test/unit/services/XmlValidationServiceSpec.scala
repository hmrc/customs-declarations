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

package unit.services

import java.io.FileNotFoundException

import org.mockito.ArgumentMatchers.{eq => ameq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.test.Helpers
import uk.gov.hmrc.customs.declaration.services.XmlValidationService
import uk.gov.hmrc.play.test.UnitSpec
import util.TestXMLData
import util.TestXMLData.{InvalidSubmissionXML, InvalidSubmissionXMLWith2Errors, ValidSubmissionXML}

import scala.xml.{Node, SAXException}

class XmlValidationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private implicit val ec = Helpers.stubControllerComponents().executionContext
  protected val MockConfiguration = mock[Configuration]
  protected val MockXml = mock[Node]

  protected val propertyName: String = "xsd.locations.submit"

  protected val xsdLocations: Seq[String] = Seq(
    "/api/conf/2.0/schemas/wco/declaration/DocumentMetaData_2_DMS.xsd",
    "/api/conf/2.0/schemas/wco/declaration/WCO_DEC_2_DMS.xsd")

  def xmlValidationService: XmlValidationService = new XmlValidationService(MockConfiguration, schemaPropertyName = propertyName){}

  override protected def beforeEach() {
    reset(MockConfiguration)
    when(MockConfiguration.getOptional[Seq[String]](propertyName)).thenReturn(Some(xsdLocations))
    when(MockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(None)
  }

  "XmlValidationService" should {
    "get location of xsd resource files from configuration" in  {
      await(xmlValidationService.validate(ValidSubmissionXML))
      verify(MockConfiguration).getOptional[Seq[String]](propertyName)
    }

    "fail the future when in configuration there are no locations of xsd resource files" in {
      when(MockConfiguration.getOptional[Seq[String]](propertyName)).thenReturn(None)

      val caught = intercept[IllegalStateException]{
        await(xmlValidationService.validate(MockXml))
      }

      caught.getMessage shouldBe s"application.conf is missing mandatory property '$propertyName'"
    }

    "fail the future when in configuration there is an empty list for locations of xsd resource files" in {
      when(MockConfiguration.getOptional[Seq[String]](propertyName)).thenReturn(Some(Nil))

      val caught = intercept[IllegalStateException] {
        await(xmlValidationService.validate(MockXml))
      }

      caught.getMessage shouldBe s"application.conf is missing mandatory property '$propertyName'"
    }

    "fail the future when a configured xsd resource file cannot be found" in {
      when(MockConfiguration.getOptional[Seq[String]](propertyName)).thenReturn(Some(List("there/is/no/such/file")))

      val caught = intercept[FileNotFoundException] {
        await(xmlValidationService.validate(MockXml))
      }

      caught.getMessage shouldBe "XML Schema resource file: there/is/no/such/file"
    }

    "successfully validate a correct xml" in {
      val result = await(xmlValidationService.validate(ValidSubmissionXML))

      result should be(())
    }

    "fail the future with SAXException when there is an error in XML" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidSubmissionXML))
      }

      caught.getMessage shouldBe "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'."

      Option(caught.getException) shouldBe None
    }

    "fail the future with wrapped SAXExceptions when there are multiple errors in XML" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidSubmissionXMLWith2Errors))
      }
      caught.getMessage shouldBe "cvc-complex-type.2.2: Element 'TotalPackageQuantity' must have no element [children], and the value must be valid."

      Option(caught.getException) shouldBe 'nonEmpty
      val wrapped1 = caught.getException
      wrapped1.getMessage shouldBe "cvc-datatype-valid.1.2.1: 'ABC' is not a valid value for 'decimal'."
      wrapped1.isInstanceOf[SAXException] shouldBe true

      Option(wrapped1.asInstanceOf[SAXException].getException) shouldBe 'nonEmpty
      val wrapped2 = wrapped1.asInstanceOf[SAXException].getException
      wrapped2.getMessage shouldBe "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'."
      wrapped2.isInstanceOf[SAXException] shouldBe true

      Option(wrapped2.asInstanceOf[SAXException].getException) shouldBe None
    }

    "fail the future with configured number of wrapped SAXExceptions when there are multiple errors in XML" in {
      when(MockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(Some(2))

      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidSubmissionXMLWith2Errors))
      }
      verify(MockConfiguration).getOptional[Int]("xml.max-errors")

      caught.getMessage shouldBe "cvc-datatype-valid.1.2.1: 'ABC' is not a valid value for 'decimal'."

      Option(caught.getException) shouldBe 'nonEmpty
      val wrapped1 = caught.getException
      wrapped1.getMessage shouldBe "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'."
      wrapped1.isInstanceOf[SAXException] shouldBe true

      Option(wrapped1.asInstanceOf[SAXException].getException) shouldBe None
    }

    "fail the future with system error when a configured maximum of xml errors is not a positive number" in {
      when(MockConfiguration.getOptional[Int]("xml.max-errors")).thenReturn(Some(0))

      val caught = intercept[IllegalArgumentException] {
        await(xmlValidationService.validate(MockXml))
      }

      caught.getMessage shouldBe "requirement failed: maxErrors should be a positive number but 0 was provided instead."
    }

    "successfully validate a cancellation request with typecode and function code" in {
      val result = await(xmlValidationService.validate(TestXMLData.validCancellationXML()))

      result should be(())
    }

  }

}
