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

package unit.services


import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{BadgeIdentifier, DeclarationsConfig}
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, StatusResponseValidationService}
import uk.gov.hmrc.play.test.UnitSpec
import util.StatusTestXMLData._
import util.TestData.{badgeIdentifier, invalidBadgeIdentifier}

import scala.xml.{Elem, NodeSeq}

class DeclarationStatusResponseValidationServiceSpec extends UnitSpec with MockitoSugar {

  protected val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
  protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
  protected val mockDeclarationsConfig: DeclarationsConfig = mock[DeclarationsConfig]
  private val statusRequestDaysLimit: Int = 60
  private val statusRequestDaysOutsideLimit: Int = statusRequestDaysLimit + 2
  private val statusRequestDaysInsideLimit: Int = statusRequestDaysLimit - 2

  private val invalidDateErrorResponse: ErrorResponse = ErrorResponse.errorBadRequest(s"Declaration acceptance date is greater than $statusRequestDaysLimit days old")
  private val invalidCreationDateErrorResponse: ErrorResponse = ErrorResponse.errorBadRequest(s"Declaration creation date is greater than $statusRequestDaysLimit days old")
  private val missingAcceptanceAndCreationDateErrorResponse: ErrorResponse = ErrorResponse.errorBadRequest("Declaration acceptanceDate and creationDate fields are missing")
  private val invalidOrMissingBadgeIdentifiersErrorResponse: ErrorResponse = ErrorResponse.errorBadRequest("Badge Identifier is missing or invalid")

  trait SetUp {

    protected lazy val service = new StatusResponseValidationService(mockLogger, mockDeclarationsConfigService)

    when(mockDeclarationsConfigService.declarationsConfig).thenReturn(mockDeclarationsConfig)
    when(mockDeclarationsConfig.declarationStatusRequestDaysLimit).thenReturn(statusRequestDaysLimit)
  }

  "StatusResponseValidationService" should {

    "return Right of true when badgeIdentifiers match and acceptance date is within configured allowed period" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, ImportTradeMovementType, InValidProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Left of Invalid Date ErrorResponse when badgeIdentifiers match and acceptance date is outside configured allowed period" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysOutsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, ImportTradeMovementType, InValidProcedureCategory), badgeIdentifier)
      result.left.get.message shouldBe invalidDateErrorResponse.message
    }

    "return Right of true when badgeIdentifiers match and creation date is within configured allowed period" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = false, ImportTradeMovementType, InValidProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Left of Invalid Date ErrorResponse when badgeIdentifiers match and creation date is outside configured allowed period" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysOutsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod,populateAcceptanceDate = false, ImportTradeMovementType, InValidProcedureCategory), badgeIdentifier)
      result.left.get.message shouldBe invalidCreationDateErrorResponse.message
    }

    "return Left of Invalid or missing Badge Identifier ErrorResponse  when badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, ImportTradeMovementType, InValidProcedureCategory), invalidBadgeIdentifier)
      result.left.get.message shouldBe invalidOrMissingBadgeIdentifiersErrorResponse.message
    }

    "return Right of true or missing Badge Identifier ErrorResponse  when badgeIdentifier cases do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, ImportTradeMovementType, InValidProcedureCategory), badgeIdentifier.copy(badgeIdentifier.value.toLowerCase))
      result shouldBe Right(true)
    }

    "return Right of true when response is tradeMovementType of EX... date is inside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, tradeMovementType = ExportTradeMovementType, procedureCategory = InValidProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, invalidBadgeIdentifier)
      result shouldBe Right(true)
    }

    "return Right of true when response is empty tradeMovementType, date is inside configured allowed period and badgeIdentifiers match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, tradeMovementType = "", procedureCategory = InValidProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Right of true when response is 1 Char tradeMovementType, date and badgeId are valid" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, tradeMovementType = "A", procedureCategory = InValidProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Right of true when response is invalid tradeMovementType, date and badgeId are valid" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, tradeMovementType = "1233JJJ", procedureCategory = InValidProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Right of true when response is tradeMovementType of CO... procedure category is ImportType, date and badgeId are valid" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, COTradeMovementType, ValidImportProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Right of true when response is tradeMovementType of CO... procedure category is empty, date and badgeId are valid" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, COTradeMovementType, ValidImportProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Left of Invalid or missing Badge Identifier ErrorResponse when response is tradeMovementType of CO... procedure category is ImportType date is inside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, COTradeMovementType, ValidImportProcedureCategory), invalidBadgeIdentifier)
      result.left.get.message shouldBe invalidOrMissingBadgeIdentifiersErrorResponse.message
    }

    "return Left of Invalid Date ErrorResponse when response is tradeMovementType of EX... date outside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysOutsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, ExportTradeMovementType, InValidProcedureCategory), invalidBadgeIdentifier)
      result.left.get.message shouldBe invalidDateErrorResponse.message
    }

    "return Left of Invalid Date ErrorResponse when response is tradeMovementType of EX... and date format is invalid" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now.minusDays(statusRequestDaysOutsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, ExportTradeMovementType, InValidProcedureCategory, dateTimeFormat = ISODateTimeFormat.dateTime()), invalidBadgeIdentifier)
      result.left.get.message shouldBe invalidDateErrorResponse.message
    }

    "return Right of true when response is tradeMovementType of CO... procedure category is ExportType, date is inside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(acceptanceOrCreationDate = dateWithinPeriod, tradeMovementType = COTradeMovementType, procedureCategory = ValidExportProcedureCategory)
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, invalidBadgeIdentifier)
      result shouldBe Right(true)
    }

    "return Right of true when response is tradeMovementType of CO... procedure category is 1 char, date and badgeId are valid" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, COTradeMovementType, "A")
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Right of true when response is tradeMovementType of CO... procedure category is invalid, date and badgeId are valid" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val xmlBody: NodeSeq = generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, COTradeMovementType, "INVALID&*T&^&^")
      val result: Either[ErrorResponse, Boolean] = service.validate(xmlBody, badgeIdentifier)
      result shouldBe Right(true)
    }

    "return Left of Invalid Date ErrorResponse when response is tradeMovementType of CO... procedure category is ExportType, date and badgeId are valid" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysOutsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, COTradeMovementType, ValidExportProcedureCategory), badgeIdentifier)
      result.left.get.message shouldBe invalidDateErrorResponse.message
    }

    "return Left of Invalid or missing Badge Identifier ErrorResponse when response is tradeMovementType of CO... procedure category is invalid, date is inside configured allowed period and badgeIdentifiers do not match" in new SetUp() {
      val dateWithinPeriod: DateTime =  DateTime.now(DateTimeZone.UTC).minusDays(statusRequestDaysInsideLimit)
      val result: Either[ErrorResponse, Boolean] = service.validate(generateDeclarationStatusResponse(dateWithinPeriod, populateAcceptanceDate = true, COTradeMovementType, InValidProcedureCategory), invalidBadgeIdentifier)
      result.left.get.message shouldBe invalidOrMissingBadgeIdentifiersErrorResponse.message
    }

    "return Left of Invalid Date ErrorResponse when xml does not contain acceptanceDate or creationDate" in new SetUp() {
      testServiceErrors(service, statusResponseDeclarationNoAcceptanceDate, badgeIdentifier, missingAcceptanceAndCreationDateErrorResponse)
    }

    "return Left of Invalid Date ErrorResponse when xml does not contain procedureCategory" in new SetUp() {
      testServiceErrors(service, statusResponseDeclarationNoProcedureCategory, badgeIdentifier, invalidDateErrorResponse)
    }

    "return Left of Invalid Date ErrorResponse when xml does not contain tradeMovementType" in new SetUp() {
      testServiceErrors(service, statusResponseDeclarationNoTradeMovementType, badgeIdentifier, invalidDateErrorResponse)
    }

    "return Left of Invalid Date ErrorResponse when response xml contains invalid acceptanceDate" in new SetUp() {
      testServiceErrors(service, statusResponseDeclarationInvalidAcceptanceDateOld, badgeIdentifier, invalidDateErrorResponse)
    }

    "return Left of Invalid Date ErrorResponse when response xml contains invalid acceptanceDate due to millis" in new SetUp() {
      testServiceErrors(service, statusResponseDeclarationInvalidAcceptanceDateWithMillis, badgeIdentifier, invalidDateErrorResponse)
    }

    "return Left of Invalid Date ErrorResponse when response xml contains invalid creationDate" in new SetUp() {
      testServiceErrors(service, statusResponseDeclarationInvalidCreationDate, badgeIdentifier, invalidCreationDateErrorResponse)
    }

    "return Left of Invalid or missing Badge Identifier ErrorResponse when response xml does not contain communicationAddress" in new SetUp() {
      testServiceErrors(service, statusResponseDeclarationNoCommunicationAddress, badgeIdentifier, invalidOrMissingBadgeIdentifiersErrorResponse)
    }

    def testServiceErrors(service: StatusResponseValidationService, xmlResponse: Elem, badgeIdentifier: BadgeIdentifier, errorResponse: ErrorResponse){
      val result: Either[ErrorResponse, Boolean] = service.validate(invalidStatusResponse(xmlResponse), badgeIdentifier)
      result.left.get.message shouldBe errorResponse.message
    }

  }


}
