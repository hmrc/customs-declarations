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

package uk.gov.hmrc.customs.declaration.xml

import org.xml.sax.{ErrorHandler, SAXParseException}

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import scala.collection.mutable
import scala.util.Try
import scala.xml.NodeSeq

class ValidateXmlAgainstSchema(schema: Schema) {

  def validate(xml: Source): Boolean =
    validateWithErrors(xml, 1) match {
      case Right(()) => true
      case Left(_) => false
    }

  def validateWithErrors(xml: Source, maxErrors: Int = Int.MaxValue): Either[List[SAXParseException], Unit] = {
    val errorHandler = new AccumulatingSAXErrorHandler(maxErrors)
    val validator = schema.newValidator()
    validator.setErrorHandler(errorHandler)
    validator.validate(xml)

    errorHandler.getAllErrors() match {
      case List() => Right(())
      case nonEmpty: List[SAXParseException] => Left(nonEmpty)
    }
  }

  private final class AccumulatingSAXErrorHandler(maxErrors: Int) extends ErrorHandler { self =>
    require(maxErrors > 0, s"maxErrors should be a positive number but $maxErrors was provided instead.")

    private lazy val errors: mutable.Buffer[SAXParseException] = mutable.Buffer.empty

    private def accumulateError(e: SAXParseException): Unit = self.synchronized {
      if (errors.size < maxErrors) {
        errors += e
      }
    }

    def getAllErrors(): List[SAXParseException] = self.synchronized { errors.toList }

    override def warning(exception: SAXParseException): Unit = {}
    override def error(exception: SAXParseException): Unit = accumulateError(exception)
    override def fatalError(exception: SAXParseException): Unit = accumulateError(exception)
  }
}

object ValidateXmlAgainstSchema {
  implicit def getXmlAsSource(xml: NodeSeq): Source = new StreamSource(new StringReader(xml.toString))

  def getSchema(path: String): Try[Schema] = Try {
    val source = new StreamSource(getClass.getResource(path).toString)
    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(source)
  }
}
