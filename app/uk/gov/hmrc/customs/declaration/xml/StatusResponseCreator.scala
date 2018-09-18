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

package uk.gov.hmrc.customs.declaration.xml

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.StatusResponse

import scala.xml._

@Singleton
class StatusResponseCreator @Inject() (declarationsLogger: DeclarationsLogger) {

  def create(statusResponse: StatusResponse): NodeSeq = {
//TODO remove blank lines when value not defined?
    <stat:declarationManagementInformationResponse xmlns:stat="http://gov.uk/customs/declarations/status-request">
      <stat:declaration>
        {if (statusResponse.versionNumber.isDefined) <stat:versionNumber>{statusResponse.versionNumber.get}</stat:versionNumber>}
        {if (statusResponse.creationDate.isDefined) <stat:creationDate>{statusResponse.creationDate.get}</stat:creationDate>}
        {if (statusResponse.goodsItemCount.isDefined) <stat:goodsItemCount>{statusResponse.goodsItemCount.get}</stat:goodsItemCount>}
        {if (statusResponse.tradeMovementType.isDefined) <stat:tradeMovementType>{statusResponse.tradeMovementType.get}</stat:tradeMovementType>}
        {if (statusResponse.declarationType.isDefined) <stat:type>{statusResponse.declarationType.get}</stat:type>}
        {if (statusResponse.packageCount.isDefined) <stat:packageCount>{statusResponse.packageCount.get}</stat:packageCount>}
        {if (statusResponse.acceptanceDate.isDefined) <stat:acceptanceDate>{statusResponse.acceptanceDate.get}</stat:acceptanceDate>}
        {statusResponse.partyIdentificationNumbers.map { maybeNumber => if (maybeNumber.isEmpty) <parties></parties> else <parties><stat:partyIdentification><stat:number>{maybeNumber.get}</stat:number></stat:partyIdentification></parties>}}
      </stat:declaration>
    </stat:declarationManagementInformationResponse>
  }

}
