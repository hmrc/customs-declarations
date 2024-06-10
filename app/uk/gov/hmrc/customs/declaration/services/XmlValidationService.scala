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

package uk.gov.hmrc.customs.declaration.services

import java.io.FileNotFoundException
import java.net.URL

import javax.inject.Inject
import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.xml.ValidateXmlAgainstSchema

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{NodeSeq, SAXException}

abstract class XmlValidationService @Inject()(val configuration: Configuration, val schemaPropertyName: String) {

  private lazy val schema: Schema = {
    def resourceUrl(resourcePath: String): URL = Option(getClass.getResource(resourcePath))
      .getOrElse(throw new FileNotFoundException(s"XML Schema resource file: $resourcePath"))

    val sources = configuration.getOptional[Seq[String]](schemaPropertyName)
      .filter(_.nonEmpty)
      .getOrElse(throw new IllegalStateException(s"application.conf is missing mandatory property '$schemaPropertyName'"))
      .map(resourceUrl(_).toString)
      .map(systemId => new StreamSource(systemId)).toArray[Source]

    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(sources)
  }

  private lazy val maxSAXErrors = configuration.getOptional[Int]("xml.max-errors").getOrElse(Int.MaxValue)

  private lazy val validator = new ValidateXmlAgainstSchema(schema)

  def validate(xml: NodeSeq)(implicit ec: ExecutionContext): Future[Unit] = {
    Future(doValidate(xml))
  }

  private def doValidate(xml: NodeSeq): Unit = {
    val source = ValidateXmlAgainstSchema.getXmlAsSource(xml)
    validator.validateWithErrors(source, maxSAXErrors) match {
      case Right(_) => ()
      case Left(errors) =>
        stackExceptions(errors).foreach(e => throw e)
    }
  }

  private def stackExceptions(exceptions: Seq[SAXException]) = {
    exceptions.foldLeft[Option[SAXException]](None) {
      (acc, nextError) => acc
          .map( currentError => new SAXException(nextError.getMessage, currentError) )
          .orElse(Some(nextError))
    }
  }
}
