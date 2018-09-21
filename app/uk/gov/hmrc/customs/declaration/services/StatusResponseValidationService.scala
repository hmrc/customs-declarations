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

  def validate(xml: NodeSeq, badgeIdentifier: BadgeIdentifier): Boolean = {
    val declarationNode = xml \ "responseDetail" \ "declarationManagementInformationResponse" \ "declaration"
    validateBadgeIdentifier(declarationNode, badgeIdentifier) && validateReceivedDate(declarationNode)
  }

  private def validateBadgeIdentifier(declarationNode: NodeSeq, badgeIdentifier: BadgeIdentifier): Boolean = {
    extractField(declarationNode, "communicationAddress").fold(false)({ communicationAddress =>
      badgeIdentifier.value == extractBadgeIdentifier(communicationAddress.head)
    })
  }

  private def extractBadgeIdentifier(communicationAddress: String) = {
    communicationAddress.split(":").last
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
