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

import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.services.{SubmissionXmlValidationService, XmlValidationService}
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.Node

abstract class XmlValidationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  protected val MockConfiguration = mock[Configuration]
  protected val MockXml = mock[Node]

  protected val schemaPropertyName: String

  protected val xsdLocations: Seq[String]

  protected def xmlValidationService(): XmlValidationService

  protected def testService(test: XmlValidationService => Unit) {
    test(xmlValidationService())
  }

  override protected def beforeEach() {
    reset(MockConfiguration)
    when(MockConfiguration.getStringSeq(schemaPropertyName)).thenReturn(Some(xsdLocations))
    when(MockConfiguration.getInt("xml.max-errors")).thenReturn(None)
  }


}
