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

import java.io.FileNotFoundException

import org.mockito.ArgumentMatchers.{eq => ameq}
import org.mockito.Mockito.{verify, when}
import uk.gov.hmrc.customs.declaration.services.CancellationXmlValidationService
import util.TestXMLData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.SAXException

//TODO MC
/*
Cancellation and Submission are subclasses of XmlValidationService - they have only one minor difference which is location .
So I would expect only one big Spec - with possibly two subclass Spec .
 */
class CancellationXmlValidationServiceSpec extends XmlValidationServiceSpec {

  override protected val xsdLocations = Seq(
    "/api/conf/2.0/schemas/wco/declaration/CANCEL_METADATA.xsd",
    "/api/conf/2.0/schemas/wco/declaration/CANCEL.xsd")

  override protected val schemaPropertyName = "xsd.locations.cancel"

  override protected def xmlValidationService() = new CancellationXmlValidationService(MockConfiguration)

  "XmlValidationService" should {
    "get location of xsd resource files from configuration" in testService { xmlValidationService =>
      await(xmlValidationService.validate(validCancellationXML()))
      verify(MockConfiguration).getStringSeq(ameq(schemaPropertyName))
    }

    "fail the future when in configuration there are no locations of xsd resource files" in testService {
      xmlValidationService =>
        when(MockConfiguration.getStringSeq(schemaPropertyName)).thenReturn(None)

        val caught = intercept[IllegalStateException] {
          await(xmlValidationService.validate(MockXml))
        }
        caught.getMessage shouldBe s"application.conf is missing mandatory property '$schemaPropertyName'"
    }

    "fail the future when in configuration there is an empty list for locations of xsd resource files" in testService {
      xmlValidationService =>
        when(MockConfiguration.getStringSeq(schemaPropertyName)).thenReturn(Some(Nil))

        val caught = intercept[IllegalStateException] {
          await(xmlValidationService.validate(MockXml))
        }
        caught.getMessage shouldBe s"application.conf is missing mandatory property '$schemaPropertyName'"
    }

    "fail the future when a configured xsd resource file cannot be found" in testService { xmlValidationService =>
      when(MockConfiguration.getStringSeq(schemaPropertyName)).thenReturn(Some(List("there/is/no/such/file")))

      val caught = intercept[FileNotFoundException] {
        await(xmlValidationService.validate(MockXml))
      }
      caught.getMessage shouldBe "XML Schema resource file: there/is/no/such/file"
    }

    "successfully validate a correct xml" in testService { xmlValidationService =>
      val result: Unit = await(xmlValidationService.validate(validCancellationXML()))
      result should be(())
    }

    "fail the future with SAXException when there is an error in XML" in testService { xmlValidationService =>
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidCancellationXML))
      }
      caught.getMessage shouldBe "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'."
      Option(caught.getException) shouldBe None
    }

    "fail the future with SAXException when payload contains incorrect type code" in testService { xmlValidationService =>
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(validCancellationXML(typeCode = "ABC")))
      }
      caught.getMessage shouldBe "cvc-type.3.1.3: The value 'ABC' of element 'TypeCode' is not valid."
      caught.getException.getMessage shouldBe "cvc-pattern-valid: Value 'ABC' is not facet-valid with respect to pattern 'INV' for type 'CancelTypeCode'."
    }


    "fail the future with SAXException when payload contains incorrect functionCode code" in testService { xmlValidationService =>
      val someFunctionCode = 9
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(validCancellationXML(functionCode = someFunctionCode)))
      }
      caught.getMessage shouldBe "cvc-type.3.1.3: The value '9' of element 'FunctionCode' is not valid."
      caught.getException.getMessage shouldBe "cvc-pattern-valid: Value '9' is not facet-valid with respect to pattern '13' for type 'FunctionCode'."
    }

    "fail the future with system error when a configured maximum of xml errors is not a positive number" in testService {
      xmlValidationService =>
        when(MockConfiguration.getInt("xml.max-errors")).thenReturn(Some(0))

        val caught = intercept[IllegalArgumentException] {
          await(xmlValidationService.validate(MockXml))
        }
        caught.getMessage shouldBe "requirement failed: maxErrors should be a positive number but 0 was provided instead."
    }

  }
}
