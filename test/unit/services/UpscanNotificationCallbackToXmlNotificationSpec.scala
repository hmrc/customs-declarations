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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.customs.declaration.services.UpscanNotificationCallbackToXmlNotification
import util.UpscanNotifyTestData._

import scala.util.control.NonFatal
import scala.xml.{Node, Utility, XML}

class UpscanNotificationCallbackToXmlNotificationSpec extends PlaySpec with MockitoSugar {

  "UpscanNotificationCallbackToXmlNotification" should {
    "transform FAILED callback payload to valid customs notification XML" in {
      val actual = new UpscanNotificationCallbackToXmlNotification().toXml(FailedCallbackBody)
      string2xml(actual.toString()) mustBe string2xml(UpscanNotificationFailedCustomsNotificationXml.toString)
    }
  }

  private def string2xml(s: String): Node = {
    val xml = try {
      XML.loadString(s)
    } catch {
      case NonFatal(thr) => fail("Not an xml: " + s, thr)
    }
    Utility.trim(xml)
  }
}
