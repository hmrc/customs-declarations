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
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.StatusResponse
import uk.gov.hmrc.customs.declaration.xml.StatusResponseCreator

import scala.xml.NodeSeq

@Singleton
class StatusResponseFilterService @Inject() (statusResponseCreator: StatusResponseCreator,
                                             declarationsLogger: DeclarationsLogger,
                                             declarationsConfigService: DeclarationsConfigService) {

  def filter(xml: NodeSeq): NodeSeq = {

    //TODO fix path
    val versionNumber = extract(xml \ "declarationManagementInformationResponse" \ "declaration" \ "versionNumber")
    val creationDate = extract (xml \ "declarationManagementInformationResponse" \ "declaration" \ "creationDate")
    val acceptanceDate = extract(xml \ "declarationManagementInformationResponse" \ "declaration" \ "acceptanceDate")
    val tradeMovementType = extract(xml \ "declarationManagementInformationResponse" \ "declaration" \ "tradeMovementType")
    val declarationType = extract(xml \ "declarationManagementInformationResponse" \ "declaration" \ "type")
    val goodsItemCount = extract(xml \ "declarationManagementInformationResponse" \ "declaration" \ "goodsItemCount")
    val packageCount = extract(xml \ "declarationManagementInformationResponse" \ "declaration" \ "packageCount")
    val parties = extractParties(xml \ "declarationManagementInformationResponse" \ "declaration" \ "parties")

    val statusResponse = StatusResponse(versionNumber, creationDate, acceptanceDate, tradeMovementType, declarationType, goodsItemCount, packageCount, parties)

    statusResponseCreator.create(statusResponse)
  }

  private def extract(xml: NodeSeq): Option[String] = {
    val value = xml.text
    if (value.isEmpty) {
      None
    } else {
      Some(value)
    }
  }

  private def extractParties(parties: NodeSeq): Option[Seq[String]] = {
    if (parties.isEmpty) {
      None
    } else {
      Some(parties.flatMap{ party => extract(party \ "partyIdentification" \ "number") })
    }
  }

}
