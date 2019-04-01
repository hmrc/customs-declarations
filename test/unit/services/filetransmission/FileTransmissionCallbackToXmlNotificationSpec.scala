/*
 * Copyright 2019 HM Revenue & Customs
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
import util.XmlOps._

class FileTransmissionCallbackToXmlNotificationSpec extends PlaySpec with MockitoSugar {

  "FileTransmissionSuccessCallbackToXmlNotification" should {
    "correctly convert success payload to xml when filename is present" in {
      stringToXml(new FileTransmissionCallbackToXmlNotification().toXml(Some(CallbackFieldsOne.name), SuccessNotification)) mustBe stringToXml(FileTransmissionSuccessCustomsNotificationXml)
    }
    "correctly convert success payload to xml when filename is not present" in {
      stringToXml(new FileTransmissionCallbackToXmlNotification().toXml(None, SuccessNotification)) mustBe stringToXml(FileTransmissionSuccessCustomsNotificationXmlWithoutFilename)
    }

    "correctly convert failure payload to xml" in {
        new FileTransmissionCallbackToXmlNotification().toXml(None, FailureNotification) mustBe FileTransmissionFailureCustomsNotificationXml
    }
  }

}
