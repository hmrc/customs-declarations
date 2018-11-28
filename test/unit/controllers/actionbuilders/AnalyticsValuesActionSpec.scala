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

package unit.controllers.actionbuilders

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsValues
import uk.gov.hmrc.customs.declaration.services.DateTimeService
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._

class AnalyticsValuesActionSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockDateTimeService: DateTimeService = mock[DateTimeService]
  }

  "AnalyticsValuesAction" can {
    "for Google Analytics" should {
      "should have the correct values setup against them" in new SetUp {
        new FileUploadAnalyticsValuesAction(mockLogger, stubUniqueIdsService, mockDateTimeService).googleAnalyticsValues shouldBe GoogleAnalyticsValues.Fileupload
        new DeclarationSubmitAnalyticsValuesAction(mockLogger, stubUniqueIdsService, mockDateTimeService).googleAnalyticsValues shouldBe GoogleAnalyticsValues.Submit
        new DeclarationClearanceAnalyticsValuesAction(mockLogger, stubUniqueIdsService, mockDateTimeService).googleAnalyticsValues shouldBe GoogleAnalyticsValues.Clearance
        new DeclarationCancellationAnalyticsValuesAction(mockLogger, stubUniqueIdsService, mockDateTimeService).googleAnalyticsValues shouldBe GoogleAnalyticsValues.Cancel
        new DeclarationAmendValuesAction(mockLogger, stubUniqueIdsService, mockDateTimeService).googleAnalyticsValues shouldBe GoogleAnalyticsValues.Amend
        new DeclarationArrivalNotificationValuesAction(mockLogger, stubUniqueIdsService, mockDateTimeService).googleAnalyticsValues shouldBe GoogleAnalyticsValues.ArrivalNotification
        new DeclarationStatusValuesAction(mockLogger, stubUniqueIdsService, mockDateTimeService).googleAnalyticsValues shouldBe GoogleAnalyticsValues.DeclarationStatus
      }
    }
  }
}
