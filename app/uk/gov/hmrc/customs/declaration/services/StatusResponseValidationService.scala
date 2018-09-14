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

  def validateBadgeIdentifier(badgeIdentifier: BadgeIdentifier, communicationAddress: String): Boolean = {
    badgeIdentifier.value == extractBadgeIdentifier(communicationAddress)
  }

  private def extractBadgeIdentifier(communicationAddress: String) = {
    communicationAddress.split(":").last
  }

  def validateReceivedDate(receiveDate: String): Boolean = {
    val parsedDateTime = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC).parseDateTime(receiveDate)
    val validDateTime = DateTime.now(DateTimeZone.UTC).minusDays(declarationsConfigService.declarationsConfig.declarationStatusRequestDaysLimit)
    parsedDateTime.isAfter(validDateTime)
  }

  //TODO check badgeId & declaration date
  def validate(xml: NodeSeq, badgeIdentifier: BadgeIdentifier): Boolean = {
    val declarationNode = xml \ "responseDetail" \ "declarationManagementInformationResponse" \ "declaration"

    val maybeCommunicationAddress = (declarationNode \ "communicationAddress").theSeq match {
      case Nil => None
      case a => Some(a.map(_.text))
    }

    val mayBeReceiveDate = (declarationNode \ "receiveDate").theSeq match {
      case Nil => None
      case a => Some(a.map(_.text))
    }

    maybeCommunicationAddress.fold(false)(address => validateBadgeIdentifier(badgeIdentifier, address.head)) &&
    mayBeReceiveDate.fold(false)(receiveDate => validateReceivedDate(receiveDate.head))

  }

}
