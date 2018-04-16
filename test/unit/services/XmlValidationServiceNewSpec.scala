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
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.services.{SubmissionXmlValidationService, XmlValidationService}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Elem, Node, SAXException}

class XmlValidationServiceNewSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val MockConfiguration = mock[Configuration]
  private val MockXml = mock[Node]
  private val xsdLocations = List(
    "/api/conf/2.0/schemas/wco/declaration/DocumentMetaData_2_DMS.xsd",
    "/api/conf/2.0/schemas/wco/declaration/CANCEL.xsd")

  def validXML(functionCode: Int = 13, typeCode: String = "INV"): Elem = <md:MetaData xmlns="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:clm63055="urn:un:unece:uncefact:codelist:standard:UNECE:AgencyIdentificationCode:D12B" xmlns:ds="urn:wco:datamodel:WCO:MetaData_DS-DMS:2" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2 ../DocumentMetaData_2_DMS.xsd ">
    <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
    <md:WCOTypeName>DEC</md:WCOTypeName>
    <md:ResponsibleCountryCode>NL</md:ResponsibleCountryCode>
    <md:ResponsibleAgencyName>Duane</md:ResponsibleAgencyName>
    <md:AgencyAssignedCustomizationVersionCode>v2.1</md:AgencyAssignedCustomizationVersionCode>
    <Declaration xmlns="urn:wco:datamodel:WCO:CANCEL-DEC-DMS:2" xmlns:clm5ISO42173A="urn:un:unece:uncefact:codelist:standard:ISO:ISO3AlphaCurrencyCode:2012-08-31" xmlns:clm63055="urn:un:unece:uncefact:codelist:standard:UNECE:AgencyIdentificationCode:D12B" xmlns:p1="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:wco:datamodel:WCO:CANCEL-DEC-DMS:2 ../CANCEL.xsd ">
      <FunctionCode>{functionCode}</FunctionCode>
      <FunctionalReferenceID>Danielle_20180404_1154</FunctionalReferenceID>
      <ID>18GBJFKYDPAB34VGO7</ID>
      <TypeCode>{typeCode}</TypeCode>
      <Submitter>
        <ID>NL025115165432</ID>
      </Submitter>
      <AdditionalInformation>
        <StatementDescription>This is a duplicate, please cancel</StatementDescription>
      </AdditionalInformation>
      <Amendment>
        <ChangeReasonCode>1</ChangeReasonCode>
      </Amendment>
    </Declaration>
  </md:MetaData>

  val InvalidXML = <md:MetaData xmlns="urn:wco:datamodel:WCO:CANCEL-DEC-DMS:2" xmlns:clm63055="urn:un:unece:uncefact:codelist:standard:UNECE:AgencyIdentificationCode:D12B" xmlns:ds="urn:wco:datamodel:WCO:MetaData_DS-DMS:2" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2 ../DocumentMetaData_2_DMS.xsd ">
    <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
    <md:WCOTypeName>DEC</md:WCOTypeName>
    <md:ResponsibleCountryCode>NL</md:ResponsibleCountryCode>
    <md:ResponsibleAgencyName>Duane</md:ResponsibleAgencyName>
    <md:AgencyAssignedCustomizationVersionCode>v2.1</md:AgencyAssignedCustomizationVersionCode>
    <Declaration foo="bar" xmlns="urn:wco:datamodel:WCO:CANCEL-DEC-DMS:2" xmlns:clm5ISO42173A="urn:un:unece:uncefact:codelist:standard:ISO:ISO3AlphaCurrencyCode:2012-08-31" xmlns:clm63055="urn:un:unece:uncefact:codelist:standard:UNECE:AgencyIdentificationCode:D12B" xmlns:p1="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:wco:datamodel:WCO:CANCEL-DEC-DMS:2 ../CANCEL.xsd ">
      <FunctionCode>13</FunctionCode>
      <FunctionalReferenceID>Danielle_20180404_1154</FunctionalReferenceID>
      <ID>18GBJFKYDPAB34VGO7</ID>
      <TypeCode>INV</TypeCode>
      <Submitter>
        <ID>NL025115165432</ID>
      </Submitter>
      <AdditionalInformation>
        <StatementDescription>This is a duplicate, please cancel</StatementDescription>
      </AdditionalInformation>
      <Amendment>
        <ChangeReasonCode>1</ChangeReasonCode>
      </Amendment>
    </Declaration>
  </md:MetaData>

  private def testService(test: XmlValidationService => Unit) {
    test(new SubmissionXmlValidationService(MockConfiguration)) //TODO MC revisit
  }

  override protected def beforeEach() {
    reset(MockConfiguration)
    when(MockConfiguration.getStringSeq("xsd.locations")).thenReturn(Some(xsdLocations))
    when(MockConfiguration.getInt("xml.max-errors")).thenReturn(None)
  }

  "XmlValidationService" should {
    "get location of xsd resource files from configuration" in testService { xmlValidationService =>
      await(xmlValidationService.validate(validXML()))
      verify(MockConfiguration).getStringSeq(ameq("xsd.locations"))
    }

    "fail the future when in configuration there are no locations of xsd resource files" in testService {
      xmlValidationService =>
        when(MockConfiguration.getStringSeq("xsd.locations")).thenReturn(None)

        val caught = intercept[IllegalStateException] {
          await(xmlValidationService.validate(MockXml))
        }
        caught.getMessage shouldBe "application.conf is missing mandatory property 'xsd.locations'"
    }

    "fail the future when in configuration there is an empty list for locations of xsd resource files" in testService {
      xmlValidationService =>
        when(MockConfiguration.getStringSeq("xsd.locations")).thenReturn(Some(Nil))

        val caught = intercept[IllegalStateException] {
          await(xmlValidationService.validate(MockXml))
        }
        caught.getMessage shouldBe "application.conf is missing mandatory property 'xsd.locations'"
    }

    "fail the future when a configured xsd resource file cannot be found" in testService { xmlValidationService =>
      when(MockConfiguration.getStringSeq("xsd.locations")).thenReturn(Some(List("there/is/no/such/file")))

      val caught = intercept[FileNotFoundException] {
        await(xmlValidationService.validate(MockXml))
      }
      caught.getMessage shouldBe "XML Schema resource file: there/is/no/such/file"
    }

    "successfully validate a correct xml" in testService { xmlValidationService =>
      val result: Unit = await(xmlValidationService.validate(validXML()))
      result should be(())
    }

    "fail the future with SAXException when there is an error in XML" in testService { xmlValidationService =>
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidXML))
      }
      caught.getMessage shouldBe "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'."
      Option(caught.getException) shouldBe None
    }

    "fail the future with SAXException when payload contains incorrect type code" in testService { xmlValidationService =>
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(validXML(typeCode = "ABC")))
      }
      caught.getMessage shouldBe "cvc-type.3.1.3: The value 'ABC' of element 'TypeCode' is not valid."
      caught.getException.getMessage shouldBe "cvc-pattern-valid: Value 'ABC' is not facet-valid with respect to pattern 'INV' for type 'CancelTypeCode'."
    }


    "fail the future with SAXException when payload contains incorrect functionCode code" in testService { xmlValidationService =>
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(validXML(functionCode = 9)))
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
