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

package unit.xml

import javax.xml.validation.Schema
import uk.gov.hmrc.customs.declaration.xml.ValidateXmlAgainstSchema
import util.UnitSpec

class ValidateXmlAgainstSchemaSpec extends UnitSpec {
  import ValidateXmlAgainstSchemaSpec._

  "ValidateXmlAgainstSchema" should {
    "load schema from file" in {
      val invalidSchema = ValidateXmlAgainstSchema.getSchema("/xml/NON_EXISTING.xsd")
      invalidSchema.isSuccess shouldBe false

      val validSchema = ValidateXmlAgainstSchema.getSchema("/xml/schema_example.xsd")
      validSchema.isSuccess shouldBe true
    }

    "not accept a max error < 1" in {
      val validator = new ValidateXmlAgainstSchema(getSchema())

      intercept[IllegalArgumentException] {
        validator.validateWithErrors(getInvalidXmlSource(), 0)
      }
    }

    "validate a valid schema" in {
      val validator = new ValidateXmlAgainstSchema(getSchema())

      validator.validate(getValidXmlSource()) shouldBe true
      validator.validateWithErrors(getValidXmlSource()).isRight shouldBe true

      validator.validate(getInvalidXmlSource()) shouldBe false
      val failure = validator.validateWithErrors(getInvalidXmlSource())
      failure.isLeft shouldBe true
    }

    "correctly limit the number of returned errors" in {
      val validator = new ValidateXmlAgainstSchema(getSchema())

      val unlimitedFailure = validator.validateWithErrors(getInvalidXmlSource())
      unlimitedFailure.isLeft shouldBe true
      unlimitedFailure.left.getOrElse(List.empty).size shouldBe 4

      val limitedFailure = validator.validateWithErrors(getInvalidXmlSource(), 2)
      limitedFailure.isLeft shouldBe true
      limitedFailure.left.getOrElse(List.empty).size shouldBe 2
    }
  }

  private def getSchema(): Schema = {
    val schema = ValidateXmlAgainstSchema.getSchema("/xml/schema_example.xsd")
    schema.isSuccess shouldBe true

    schema.get
  }

  private def getValidXmlSource() = ValidateXmlAgainstSchema.getXmlAsSource(validXML)
  private def getInvalidXmlSource() = ValidateXmlAgainstSchema.getXmlAsSource(invalidXML)
}

object ValidateXmlAgainstSchemaSpec {
  val validXML = <shiporder orderid="889923"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:noNamespaceSchemaLocation="schema_example.xsd">
    <orderperson>John Smith</orderperson>
    <shipto>
      <name>Ola Nordmann</name>
      <address>Langgt 23</address>
      <city>4000 Stavanger</city>
      <country>Norway</country>
    </shipto>
    <item>
      <title>Empire Burlesque</title>
      <note>Special Edition</note>
      <quantity>1</quantity>
      <price>10.90</price>
    </item>
    <item>
      <title>Hide your heart</title>
      <quantity>1</quantity>
      <price>9.90</price>
    </item>
  </shiporder>

  val invalidXML = <shiporder
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:noNamespaceSchemaLocation="schema_example.xsd">
    <orderperson>John Smith</orderperson>
    <undefinedelement>not supposed to be here</undefinedelement>
    <shipto>
      <name>Ola Nordmann</name>
      <address>Langgt 23</address>
      <city>4000 Stavanger</city>
      <country>Norway</country>
    </shipto>
    <item>
      <title>Empire Burlesque</title>
      <note>Special Edition</note>
      <quantity>1</quantity>
      <price>10.90</price>
    </item>
    <item>
      <title>Hide your heart</title>
      <quantity>-5</quantity>
      <price>9.90</price>
    </item>
  </shiporder>
}
