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


import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.DeclarationsConfig
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, StatusResponseValidationService}
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData.{badgeIdentifier, invalidBadgeIdentifier}
import util.TestXMLData._

class StatusResponseValidationServiceSpec extends UnitSpec with MockitoSugar {

  protected val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
  protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
  protected val mockDeclarationsConfig: DeclarationsConfig = mock[DeclarationsConfig]
  private val statusRequestDaysLimit: Int = 60

  trait SetUp {

    protected lazy val service = new StatusResponseValidationService(mockLogger, mockDeclarationsConfigService)

    when(mockDeclarationsConfigService.declarationsConfig).thenReturn(mockDeclarationsConfig)
    when(mockDeclarationsConfig.declarationStatusRequestDaysLimit).thenReturn(statusRequestDaysLimit)
  }

  "StatusResponseValidationService" should {

    "return true when badgeIdentifiers match and date is within configured allowed period" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysLimit - 1)
      val result: Boolean = service.validate(validStatusResponse(dateWithinPeriod.toString), badgeIdentifier)
      result shouldBe true
    }

    "return false when badgeIdentifiers match and date is outside configured allowed period" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysLimit + 1)
      val result: Boolean = service.validate(validStatusResponse(dateWithinPeriod.toString), badgeIdentifier)
      result shouldBe false
    }

    "return false when badgeIdentifiers do not match" in new SetUp() {
      val result: Boolean = service.validate(validStatusResponse(), invalidBadgeIdentifier)
      result shouldBe false
    }

    "return false when xml does not contain receivedDate" in new SetUp() {
      val result: Boolean = service.validate(invalidStatusResponse(statusResponseDeclarationXmlNodeNoDate), badgeIdentifier)
      result shouldBe false
    }

    "return false when xml contains invalid receivedDate" in new SetUp() {
      val result: Boolean = service.validate(invalidStatusResponse(statusResponseDeclarationXmlNodeInvalidDate), badgeIdentifier)
      result shouldBe false
    }

    "return false when xml does not contain communicationAddress" in new SetUp() {
      val result: Boolean = service.validate(invalidStatusResponse(statusResponseDeclarationXmlNodeCommunicationAddress), badgeIdentifier)
      result shouldBe false
    }

  }

}
