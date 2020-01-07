/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.services.filetransmission

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.customs.declaration.services.filetransmission.FileTransmissionCallbackToXmlNotification
import util.FileTransmissionTestData._
import util.TestData._

class FileTransmissionCallbackToXmlNotificationSpec extends PlaySpec with MockitoSugar {

  // Removal of string2xml is deliberate to ensure blank lines are not output.

  "FileTransmissionSuccessCallbackToXmlNotification" should {
    "correctly convert success payload to xml when filename is present" in {
      val actual = new FileTransmissionCallbackToXmlNotification().toXml(Some(CallbackFieldsOne.name), SuccessNotification)
      actual mustBe FileTransmissionSuccessCustomsNotificationXml
    }
    "correctly convert success payload to xml when filename is not present" in {
      val actual = new FileTransmissionCallbackToXmlNotification().toXml(None, SuccessNotification)
      actual mustBe FileTransmissionSuccessCustomsNotificationXmlWithoutFilename
    }

    "correctly convert failure payload to xml when filename is present" in {
      val actual = new FileTransmissionCallbackToXmlNotification().toXml(Some(CallbackFieldsOne.name), FailureNotification)
      actual mustBe FileTransmissionFailureCustomsNotificationXml
    }
    "correctly convert failure payload to xml when filename is not present" in {
      val actual = new FileTransmissionCallbackToXmlNotification().toXml(None, FailureNotification)
      actual mustBe FileTransmissionFailureCustomsNotificationXmlWithoutFilename
    }
  }
}
