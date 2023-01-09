/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.services.upscan

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.customs.declaration.services.upscan.UpscanNotificationCallbackToXmlNotification
import util.UpscanNotifyTestData._
import util.XmlOps.stringToXml

class UpscanNotificationCallbackToXmlNotificationSpec extends PlaySpec with MockitoSugar {

  "UpscanNotificationCallbackToXmlNotification" should {
    "transform FAILED callback payload to valid customs notification XML" in {
      val actual = new UpscanNotificationCallbackToXmlNotification().toXml(None, FailedCallbackBody)

      stringToXml(actual.toString()) mustBe stringToXml(UpscanNotificationFailedCustomsNotificationXml.toString)
    }
  }

}
