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

package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.BadgeIdentifier

import scala.util.Try
import scala.xml.NodeSeq

@Singleton
class StatusResponseValidationService @Inject() (declarationsLogger: DeclarationsLogger, declarationsConfigService: DeclarationsConfigService) {

  val IMPORT_MOVEMENT_TYPE : String = "IM"
  val EXPORT_MOVEMENT_TYPE : String = "EX"
  val CO_MOVEMENT_TYPE : String = "CO"

  val importProcedureCategories: Seq[String] = Seq( "40", "42", "61", "07", "51", "53", "71")
  val exportProcedureCategories: Seq[String] = Seq( "10")

  val ISO_UTC_DateTimeFormat_noMillis: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC()

  def validate(xml: NodeSeq, badgeIdentifier: BadgeIdentifier): Either[ErrorResponse, Boolean] = {
    val declarationNode = xml \ "responseDetail" \ "declarationManagementInformationResponse" \ "declaration"
    val theResult =  for {
      tradeMovementType <- extractTradeMovementType(declarationNode).right
      xmlNode <- handleValidationForTradeTypes(badgeIdentifier, tradeMovementType, declarationNode).right
    } yield xmlNode
    theResult
  }

  def handleValidationForTradeTypes(badgeIdentifier: BadgeIdentifier, tradeMovementType: String, declarationNode: NodeSeq): Either[ErrorResponse, Boolean] ={
    tradeMovementType match {
      case EXPORT_MOVEMENT_TYPE => validateAcceptanceDate(declarationNode)
      case _ => validateImports(badgeIdentifier, declarationNode)
    }
  }

  private def validateImports(badgeIdentifier: BadgeIdentifier, declarationNode: NodeSeq): Either[ErrorResponse, Boolean] = {
    val result: Either[ErrorResponse, Boolean] = for {
      _ <- validateBadgeIdentifier(declarationNode, badgeIdentifier).right
      _ <- validateAcceptanceDate(declarationNode).right
    } yield true
    result
  }

  private def validateBadgeIdentifier(declarationNode: NodeSeq, badgeIdentifier: BadgeIdentifier): Either[ErrorResponse, Boolean] = {
    extractField(declarationNode, "communicationAddress").fold[Either[ErrorResponse, Boolean]]({
      declarationsLogger.errorWithoutRequestContext("Status response BadgeId field is missing")
      Left(ErrorResponse.errorBadRequest("Badge Identifier is missing or invalid"))
    })({ communicationAddress =>
      if(badgeIdentifier.value.toUpperCase != extractBadgeIdentifier(communicationAddress.head).toUpperCase){
        Left(ErrorResponse.errorBadRequest("Badge Identifier is missing or invalid"))
      }else{
        Right(true)
      }
    })
  }

  private def validateAcceptanceDate(declarationNode: NodeSeq): Either[ErrorResponse, Boolean] = extractField(declarationNode, "acceptanceDate").fold[Either[ErrorResponse, Boolean]]({
    declarationsLogger.errorWithoutRequestContext("Status response acceptanceDate field is missing")
    Left(ErrorResponse.errorBadRequest(s"Declaration acceptance date is greater than ${declarationsConfigService.declarationsConfig.declarationStatusRequestDaysLimit} days old"))
  })(acceptanceDate => {
    val parsedDateTime = Try(ISO_UTC_DateTimeFormat_noMillis.parseDateTime(acceptanceDate.head)).toOption
    val isDateValid = parsedDateTime.fold(false)(validDateTime => validDateTime.isAfter(getValidDateTimeUsingConfig))
    if (!isDateValid) {
      declarationsLogger.debugWithoutRequestContext(s"Status response acceptanceDate failed validation $acceptanceDate")
      Left(ErrorResponse.errorBadRequest(s"Declaration acceptance date is greater than ${declarationsConfigService.declarationsConfig.declarationStatusRequestDaysLimit} days old"))
    } else{
      Right(true)
    }
  })

  private def safelyExtractValue(extractedValues : Seq[String]): Option[String] = {
    if(extractedValues.nonEmpty && !extractedValues.head.isEmpty && extractedValues.head.length > 1) {
      Some(extractedValues.head.substring(0,2))
    } else {
      None
    }
  }

  def extractTradeMovementType(declarationNode: NodeSeq): Either[ErrorResponse, String]  = {
    val maybeExtractedTradeMovementType = extractField(declarationNode, "tradeMovementType").fold[Option[String]](None)(tradeMovementType => {
      safelyExtractValue(tradeMovementType)
    })

    maybeExtractedTradeMovementType.fold[Either[ErrorResponse, String]]({
      Right(IMPORT_MOVEMENT_TYPE)
    })(extractedTradeMovementType => {
      if(extractedTradeMovementType.equals(CO_MOVEMENT_TYPE)) {
        Right(deriveTradeMovementTypeFromProcedureCategory(extractProcedureCategory(declarationNode)))
      } else{
        Right(extractedTradeMovementType)
      }
    })
  }

  private def deriveTradeMovementTypeFromProcedureCategory(maybeProcedureCategory: Option[String]):  String  = {
    if(isImportProcedureCategory(maybeProcedureCategory)){
      IMPORT_MOVEMENT_TYPE
    } else if(isExportProcedureCategory(maybeProcedureCategory)){
      EXPORT_MOVEMENT_TYPE
    } else{
      IMPORT_MOVEMENT_TYPE
    }
  }

  private def isExportProcedureCategory(maybeProcedureCategory: Option[String]): Boolean = {
    matchAgainstGivenCategories(maybeProcedureCategory, exportProcedureCategories)
  }

  private def isImportProcedureCategory(maybeProcedureCategory: Option[String]): Boolean = {
    matchAgainstGivenCategories(maybeProcedureCategory, importProcedureCategories)
  }

  private def matchAgainstGivenCategories(maybeProcedureCategory: Option[String], categoriesToMatch: Seq[String]): Boolean ={
    maybeProcedureCategory.exists(procedureCategory => categoriesToMatch.contains(procedureCategory))
  }

  private def extractProcedureCategory(declarationNode: NodeSeq): Option[String]  = {
    extractField(declarationNode, "procedureCategory").fold[Option[String]](None)(procedureCategory => safelyExtractValue(procedureCategory))
  }

  private def extractBadgeIdentifier(communicationAddress: String) = {
    communicationAddress.split(":").last
  }

  private def getValidDateTimeUsingConfig = {
    DateTime.now(DateTimeZone.UTC).minusDays(declarationsConfigService.declarationsConfig.declarationStatusRequestDaysLimit)
  }

  private def extractField(declarationNode: NodeSeq, nodeName: String): Option[Seq[String]] = {
    val mayBeFieldValue = (declarationNode \ nodeName).theSeq match {
      case Nil => None
      case a => Some(a.map(_.text))
    }
    mayBeFieldValue
  }
}
