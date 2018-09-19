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
import uk.gov.hmrc.customs.declaration.model.{BadgeIdentifier, DeclarationsConfig}
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, StatusResponseValidationService}
import uk.gov.hmrc.play.test.UnitSpec
import util.StatusTestXMLData._
import util.TestData.{badgeIdentifier, invalidBadgeIdentifier}

import scala.xml.Elem

class StatusResponseValidationServiceSpec extends UnitSpec with MockitoSugar {

  protected val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
  protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
  protected val mockDeclarationsConfig: DeclarationsConfig = mock[DeclarationsConfig]
  private val statusRequestDaysLimit: Int = 60
  private val statusRequestDaysOutsideLimit: Int = statusRequestDaysLimit + 2
  private val statusRequestDaysInsideLimit: Int = statusRequestDaysLimit - 2


  trait SetUp {

    protected lazy val service = new StatusResponseValidationService(mockLogger, mockDeclarationsConfigService)

    when(mockDeclarationsConfigService.declarationsConfig).thenReturn(mockDeclarationsConfig)
    when(mockDeclarationsConfig.declarationStatusRequestDaysLimit).thenReturn(statusRequestDaysLimit)
  }

  "StatusResponseValidationService" should {

    "validate should return true when badgeIdentifiers match and date is within configured allowed period" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, ImportTradeMovementType, InValidProcedureCategory), badgeIdentifier)
      result shouldBe true
    }

    "validate should return false when badgeIdentifiers match and date is outside configured allowed period" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysOutsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, ImportTradeMovementType, InValidProcedureCategory), badgeIdentifier)
      result shouldBe false
    }

    "validate should return false when badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, ImportTradeMovementType, InValidProcedureCategory), invalidBadgeIdentifier)
      result shouldBe false
    }

    "validate should return true when response is tradeMovementType of EX... date is inside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, ExportTradeMovementType, InValidProcedureCategory), invalidBadgeIdentifier)
      result shouldBe true
    }

    "validate should return true when response is tradeMovementType of CO... procedure category is ImportType date is inside configured allowed period and badgeIdentifiers match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, COTradeMovementType, ValidImportProcedureCategory), badgeIdentifier)
      result shouldBe true
    }

    "validate should return false when response is tradeMovementType of CO... procedure category is ImportType date is inside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, COTradeMovementType, ValidImportProcedureCategory), invalidBadgeIdentifier)
      result shouldBe false
    }

    "validate should return false when response is tradeMovementType of EX... date outside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysOutsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, ExportTradeMovementType, InValidProcedureCategory), invalidBadgeIdentifier)
      result shouldBe false
    }

    "validate should return true when response is tradeMovementType of CO... procedure category is ExportType, date is inside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, COTradeMovementType, ValidExportProcedureCategory), invalidBadgeIdentifier)
      result shouldBe true
    }

    "validate should return false when response is tradeMovementType of CO... procedure category is ExportType, date is outside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysOutsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, COTradeMovementType, ValidExportProcedureCategory), badgeIdentifier)
      result shouldBe false
    }

    "validate should return false when response is tradeMovementType of CO... procedure category is invalid, date is inside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Boolean = service.validate(generateDeclarationManagementInformationResponse(dateWithinPeriod.toString, COTradeMovementType, InValidProcedureCategory), badgeIdentifier)
      result shouldBe false
    }

    "validate should return false when xml does not contain receivedDate" in new SetUp() {
      testService(service, statusResponseDeclarationNoReceiveDate, badgeIdentifier, expectedResult = false)
    }

    "validate should return false when xml does not contain procedureCategory" in new SetUp() {
      testService(service, statusResponseDeclarationNoProcedureCategory, badgeIdentifier, expectedResult = false)
    }

    "validate should return false when xml does not contain tradeMovementType" in new SetUp() {
      testService(service, statusResponseDeclarationNoTradeMovementType, badgeIdentifier, expectedResult = false)
    }

    "validate should return false when response xml contains invalid receivedDate" in new SetUp() {
      testService(service, statusResponseDeclarationInvalidReceiveDate, badgeIdentifier, expectedResult = false)
    }

    "validate should return false when response xml does not contain communicationAddress" in new SetUp() {
      testService(service, statusResponseDeclarationNoCommunicationAddress, badgeIdentifier, expectedResult = false)
    }

    "validate should return false when response xml does not contain a valid communicationAddress" in new SetUp() {
      testService(service, statusResponseDeclarationCommunicationAddressFormatInvalid, badgeIdentifier, expectedResult = false)
    }

    def testService(service: StatusResponseValidationService, xmlResponse: Elem, badgeIdentifier: BadgeIdentifier, expectedResult: Boolean){
      val result: Boolean = service.validate(invalidStatusResponse(xmlResponse), badgeIdentifier)
      result shouldBe expectedResult
    }


  }


}
