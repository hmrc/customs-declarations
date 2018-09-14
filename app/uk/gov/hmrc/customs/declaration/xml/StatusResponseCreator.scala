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

import javax.inject.Singleton
import uk.gov.hmrc.customs.declaration.model.StatusResponse

import scala.xml._

@Singleton
class StatusResponseCreator {

  //TODO tests
  def create(statusResponse: StatusResponse): NodeSeq = {

    <stat:declarationManagementInformationResponse xmlns:stat="http://gov.uk/customs/declarations/status-request">
      <stat:declaration>
        {if (statusResponse.versionNumber.isDefined) <stat:versionNumber>{statusResponse.versionNumber}</stat:versionNumber>}
        {if (statusResponse.creationDate.isDefined) <stat:creationDate formatCode="string">{statusResponse.creationDate}</stat:creationDate>}
        {if (statusResponse.goodsItemCount.isDefined) <stat:goodsItemCount unitType="string" qualifier="string">{statusResponse.goodsItemCount}</stat:goodsItemCount>}
        {if (statusResponse.tradeMovementType.isDefined) <stat:tradeMovementType type="token" responsibleAgent="token">{statusResponse.tradeMovementType}</stat:tradeMovementType>}
        {if (statusResponse.declarationType.isDefined) <stat:type type="token" responsibleAgent="token">{statusResponse.declarationType}</stat:type>}
        {if (statusResponse.packageCount.isDefined) <stat:packageCount unitType="string" qualifier="string">{statusResponse.packageCount}</stat:packageCount>}
        {if (statusResponse.acceptanceDate.isDefined) <stat:acceptanceDate formatCode="string">{statusResponse.acceptanceDate}</stat:acceptanceDate>}
        {if (statusResponse.partyIdentificationNumbers.isDefined) {
          <stat:parties>
            {statusResponse.partyIdentificationNumbers.get.map { value =>
            <stat:partyIdentification>
              <stat:number>
                {value}
              </stat:number>
            </stat:partyIdentification>}}
          </stat:parties>
        }}
      </stat:declaration>
    </stat:declarationManagementInformationResponse>
  }

}
