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

    val path = xml \ "responseDetail" \ "declarationManagementInformationResponse" \ "declaration"
    val versionNumber = extract(path \ "versionNumber")
    val creationDate = extract (path \ "creationDate")
    val goodsItemCount = extract(path \ "goodsItemCount")
    val tradeMovementType = extract(path \ "tradeMovementType")
    val declarationType = extract(path \ "type")
    val packageCount = extract(path \ "packageCount")
    val acceptanceDate = extract(path \ "acceptanceDate")
    val parties = extractParties(path \ "parties")

    val statusResponse = StatusResponse(versionNumber, creationDate, goodsItemCount, tradeMovementType,
      declarationType, packageCount, acceptanceDate, parties)

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

  private def extractParties(parties: NodeSeq): Option[Seq[Option[String]]] = {
    if (parties.isEmpty) {
      None
    } else {
      Some(parties.map(party => extract(party \ "partyIdentification" \ "number")))
    }
  }

}
