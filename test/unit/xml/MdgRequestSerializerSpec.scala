/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.xml.sax.SAXParseException
import uk.gov.hmrc.customs.declaration.xml.MdgRequestSerializer
import util.Dec64TestData.*

import scala.util.Try
import scala.xml.factory.XMLLoader
import scala.xml.parsing.FactoryAdapter
import scala.xml.{Elem, SAXParser, Utility, XML}

class MdgRequestSerializerSpec extends AnyWordSpec with Matchers {

  private val serializer = new MdgRequestSerializer()
  private val placeholder = MdgRequestSerializer.FilenamePlaceholder

  "MdgRequestSerializer" should {

    "produce XML that is valid against the MDG BatchFileInterfaceMetadata-1.0.7 schema" in {
      val result = validateSchema(serializer.serialize(Dec64SubmissionOne, PresignedUrl, FileSize))
      withClue(result) { result.isSuccess shouldBe true }
    }

    "leave out the properties element, and stay schema-valid, when there are no properties" in {
      val actual = serializer.serialize(Dec64SubmissionOne.copy(properties = Nil), PresignedUrl, FileSize)
      actual should not include "properties"
      val result = validateSchema(actual)
      withClue(result) { result.isSuccess shouldBe true }
    }

    "populate the same values as file-transmission, with the presigned URL and real file size" in {
      val s = Dec64SubmissionOne
      val expected =
        s"""<mdg:BatchFileInterfaceMetadata xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:mdg="http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema" xsi:schemaLocation="http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema BatchFileInterfaceMetadata-1.0.7.xsd">
           |    <mdg:sourceSystem>MDTP</mdg:sourceSystem>
           |    <mdg:sourceSystemType>AWS</mdg:sourceSystemType>
           |    <mdg:interfaceName>DEC64</mdg:interfaceName>
           |    <mdg:interfaceVersion>1.0.0</mdg:interfaceVersion>
           |    <mdg:correlationID>${s.fileReference}</mdg:correlationID>
           |    <mdg:sequenceNumber>1</mdg:sequenceNumber>
           |    <mdg:batchID>${s.batchId}</mdg:batchID>
           |    <mdg:batchSize>2</mdg:batchSize>
           |    <mdg:batchCount>1</mdg:batchCount>
           |    <mdg:extractEndDateTime>2018-04-24T09:30:00Z</mdg:extractEndDateTime>
           |    <mdg:checksum>$ValidChecksumHex</mdg:checksum>
           |    <mdg:checksumAlgorithm>SHA-256</mdg:checksumAlgorithm>
           |    <mdg:fileSize>2048</mdg:fileSize>
           |    <mdg:compressed>false</mdg:compressed>
           |    <mdg:encrypted>false</mdg:encrypted>
           |    <mdg:properties>
           |        <mdg:property><mdg:name>DeclarationId</mdg:name><mdg:value>decId123</mdg:value></mdg:property>
           |        <mdg:property><mdg:name>Eori</mdg:name><mdg:value>ZZ123456789000</mdg:value></mdg:property>
           |        <mdg:property><mdg:name>DocumentType</mdg:name><mdg:value>Document Type 1</mdg:value></mdg:property>
           |    </mdg:properties>
           |    <mdg:sourceLocation>https://object-store.s3.example/dec64/file?X-Amz-Algorithm=AWS4-HMAC-SHA256&amp;X-Amz-Expires=900&amp;X-Amz-Signature=abc123</mdg:sourceLocation>
           |    <mdg:sourceFileName>someFile.pdf</mdg:sourceFileName>
           |    <mdg:sourceFileMimeType>application/pdf</mdg:sourceFileMimeType>
           |    <mdg:destinations>
           |        <mdg:destination>
           |            <mdg:destinationSystem>CDS</mdg:destinationSystem>
           |        </mdg:destination>
           |    </mdg:destinations>
           |</mdg:BatchFileInterfaceMetadata>""".stripMargin

      val actual = serializer.serialize(s, PresignedUrl, FileSize)

      Utility.trim(XML.loadString(actual)) shouldBe Utility.trim(XML.loadString(expected))
    }

    "use the fileSize argument, not the size stored on the submission" in {
      val actual = serializer.serialize(Dec64SubmissionOne.copy(size = None), PresignedUrl, 99L)
      (XML.loadString(actual) \ "fileSize").text shouldBe "99"
    }

    "escape XML special characters in the file name and presigned URL" in {
      val name = "a&b <c> \"d\".pdf"
      val actual = serializer.serialize(Dec64SubmissionOne.copy(fileName = name), PresignedUrl, FileSize)

      actual should include("X-Amz-Algorithm=AWS4-HMAC-SHA256&amp;X-Amz-Expires=900")
      validateSchema(actual).isSuccess shouldBe true
      val parsed = XML.loadString(actual)
      (parsed \ "sourceFileName").text shouldBe name
      (parsed \ "sourceLocation").text shouldBe PresignedUrl.toString
    }

    "not add whitespace to long fields" in {
      val longFilename = "a" * 200 + ".pdf"
      val actual = serializer.serialize(Dec64SubmissionOne.copy(fileName = longFilename), PresignedUrl, FileSize)
      actual should include(s"<mdg:sourceFileName>$longFilename</mdg:sourceFileName>")
    }

    "add the file extension when the file name is the upscan placeholder" in {
      val actual = serializer.serialize(Dec64SubmissionOne.copy(fileName = placeholder), PresignedUrl, FileSize)
      actual should include(s"<mdg:sourceFileName>$placeholder.pdf</mdg:sourceFileName>")
    }

    "leave the placeholder unchanged when the mime type has no known extension" in {
      val actual = serializer.serialize(Dec64SubmissionOne.copy(fileName = placeholder, mimeType = "application/zip"), PresignedUrl, FileSize)
      actual should include(s"<mdg:sourceFileName>$placeholder</mdg:sourceFileName>")
    }
  }

  private def validateSchema(body: String): Try[Unit] = {
    val schemaLang = javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
    val xsdStream = javax.xml.transform.stream.StreamSource(getClass.getResourceAsStream("/xml/BatchFileInterfaceMetadata-1.0.7.xsd"))
    val schema = javax.xml.validation.SchemaFactory.newInstance(schemaLang).newSchema(xsdStream)

    val factory = javax.xml.parsers.SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)
    factory.setSchema(schema)

    val validatingParser = factory.newSAXParser()
    val xmlLoader: XMLLoader[Elem] = new XMLLoader[Elem] {
      override def parser: SAXParser = validatingParser
      override def adapter: FactoryAdapter = new scala.xml.parsing.NoBindingFactoryAdapter {
        override def error(e: SAXParseException): Unit = throw e
      }
    }

    Try(xmlLoader.loadString(body)).map(_ => ())
  }
}
