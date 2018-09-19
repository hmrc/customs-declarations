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
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.BadgeIdentifier

import scala.xml.NodeSeq

@Singleton
class StatusResponseValidationService @Inject() (declarationsLogger: DeclarationsLogger, declarationsConfigService: DeclarationsConfigService) {

  val IMPORT_MOVEMENT_TYPE : String = "IM"
  val EXPORT_MOVEMENT_TYPE : String = "EX"
  val CO_MOVEMENT_TYPE : String = "CO"

  val importProcedureCategories: Seq[String] = Seq( "40", "42", "61", "07", "51", "53", "71")
  val exportProcedureCategories: Seq[String] = Seq( "10")

 // tradeMovementType procedureCategory

  def validate(xml: NodeSeq, badgeIdentifier: BadgeIdentifier): Boolean = {
    val declarationNode = xml \ "responseDetail" \ "declarationManagementInformationResponse" \ "declaration"
    extractMovementType(declarationNode) match {
      case Some(IMPORT_MOVEMENT_TYPE) => (validateBadgeIdentifier(declarationNode, badgeIdentifier) && validateReceivedDate(declarationNode))
      case Some(EXPORT_MOVEMENT_TYPE) => validateReceivedDate(declarationNode)
      case Some(_) => false
      case None => false
    }
  }


  def extractMovementType(declarationNode: NodeSeq): Option[String]  = {
    val mayBetradeMovementType = extractField(declarationNode, "tradeMovementType").fold[Option[String]](None)(tradeMovementType => Some(tradeMovementType.head.substring(0,2)))
    if(mayBetradeMovementType.isDefined && mayBetradeMovementType.get == CO_MOVEMENT_TYPE) {
      Some(deriveTradeMovementTypeFromProcedureCategory(extractProcedureCategory(declarationNode)))
    } else {
      mayBetradeMovementType
    }
  }

  private def deriveTradeMovementTypeFromProcedureCategory(maybeProcedureCategory: Option[String]):  String  = {
     if(isImportProcedureCategory(maybeProcedureCategory)){
       IMPORT_MOVEMENT_TYPE
     } else if(isExportProcedureCategory(maybeProcedureCategory)){
        EXPORT_MOVEMENT_TYPE
     } else{
       CO_MOVEMENT_TYPE
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
    extractField(declarationNode, "procedureCategory").fold[Option[String]](None)(procedureCategory => Some(procedureCategory.head.substring(0,2)))
  }

  private def validateBadgeIdentifier(declarationNode: NodeSeq, badgeIdentifier: BadgeIdentifier): Boolean = {
    extractField(declarationNode, "communicationAddress").fold(false)({ communicationAddress =>
      validateCommunicationAddress(communicationAddress.head) && badgeIdentifier.value == extractBadgeIdentifier(communicationAddress.head)
    })
  }

  private def extractBadgeIdentifier(communicationAddress: String) = {
    communicationAddress.split(":").last
  }

  private def validateCommunicationAddress(communicationAddress: String): Boolean = {
    val regexString = "hmrcgwid:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{16}:[0-9a-zA-Z]{10}"
    val isCommunicationsAddressValid = communicationAddress.matches(regexString)
    if(!isCommunicationsAddressValid) declarationsLogger.debugWithoutRequestContext(s"Status response communicationsAddress failed validation $communicationAddress")
    isCommunicationsAddressValid
  }

  private def validateReceivedDate(declarationNode: NodeSeq): Boolean = {
    extractField(declarationNode, "receiveDate").fold(false)(receiveDate => {
     val parsedDateTime = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC).parseDateTime(receiveDate.head)
     val isDateValid = parsedDateTime.isAfter(getValidDateTimeUsingConfig)
     if (!isDateValid) declarationsLogger.debugWithoutRequestContext(s"Status response receivedDate failed validation $receiveDate")
     isDateValid
   })
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
