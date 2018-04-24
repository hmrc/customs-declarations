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

package uk.gov.hmrc.customs.declaration.services

import java.io.{FileNotFoundException, StringReader}
import java.net.URL

import javax.inject.{Inject, Singleton}
import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import org.xml.sax.{ErrorHandler, SAXParseException}
import play.api.Configuration

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{NodeSeq, SAXException}

@Singleton
abstract class XmlValidationService @Inject()(configuration: Configuration) {

  protected val schemaPath: String

  private lazy val schema: Schema = {
    def resourceUrl(resourcePath: String): URL = Option(getClass.getResource(resourcePath))
      .getOrElse(throw new FileNotFoundException(s"XML Schema resource file: $resourcePath"))

    val sources = configuration.getStringSeq(schemaPath)
      .filter(_.nonEmpty)
      .getOrElse(throw new IllegalStateException(s"application.conf is missing mandatory property '$schemaPath'"))
      .map(resourceUrl(_).toString)
      .map(systemId => new StreamSource(systemId)).toArray[Source]

    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(sources)
  }

  private lazy val maxSAXErrors = configuration.getInt("xml.max-errors").getOrElse(Int.MaxValue)

  def validate(xml: NodeSeq)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(doValidate(xml))
  }

  private def doValidate(xml: NodeSeq): Unit = {
    val errorHandler = new AccumulatingSAXErrorHandler(maxSAXErrors)
    val validator = schema.newValidator()
    validator.setErrorHandler(errorHandler)
    validator.validate(new StreamSource(new StringReader(xml.toString)))
    errorHandler.throwIfErrorsEncountered()
  }

  private class AccumulatingSAXErrorHandler(maxErrors: Int) extends ErrorHandler {
    self =>

    require(maxErrors > 0, s"maxErrors should be a positive number but $maxErrors was provided instead.")

    private lazy val errors: mutable.Buffer[SAXParseException] = mutable.Buffer.empty

    private def accumulateError(e: SAXParseException): Unit = self.synchronized {
      errors += e
      if (errors.lengthCompare(maxErrors) >= 0) throwIfErrorsEncountered()
    }

    def throwIfErrorsEncountered(): Unit = self.synchronized {
      val maybeTotalException = errors.foldLeft[Option[SAXException]](None) {
        (acc, nextError) =>
          acc
            .map { currentError => new SAXException(nextError.getMessage, currentError) }
            .orElse(Some(nextError))
      }
      maybeTotalException.foreach(e => throw e)
    }

    override def warning(exception: SAXParseException): Unit = {}

    override def error(exception: SAXParseException): Unit = accumulateError(exception)

    override def fatalError(exception: SAXParseException): Unit = accumulateError(exception)
  }

}
