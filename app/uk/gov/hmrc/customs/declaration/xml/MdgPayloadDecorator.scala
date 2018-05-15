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

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import uk.gov.hmrc.customs.declaration.model.{Csp, FieldsId, SubscriptionFieldsId}
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest

import scala.xml.NodeSeq

class MdgPayloadDecorator() {

  def wrap[A](xml: NodeSeq, clientId: FieldsId, dateTime: DateTime)(implicit vpr: ValidatedPayloadRequest[A]): NodeSeq =
    <v1:submitDeclarationRequest
    xmlns:v1="http://uk/gov/hmrc/mdg/declarationmanagement/submitdeclaration/request/schema/v1"
    xmlns:n1="urn:wco:datamodel:WCO:DEC-DMS:2"
    xmlns:p1="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <v1:requestCommon>
        <!--type: regimeType-->
        <v1:regime>CDS</v1:regime>
        <v1:receiptDate>{ dateTime.toString(ISODateTimeFormat.dateTimeNoMillis) }</v1:receiptDate>
        <v1:clientID>{clientId}</v1:clientID>
        <v1:conversationID>{vpr.conversationId.uuid}</v1:conversationID>
        { vpr.authorisedAs match {
            case Csp(badgeId) => <v1:badgeIdentifier>{badgeId.value}</v1:badgeIdentifier>
            case _ => NodeSeq.Empty
          }
        }
      </v1:requestCommon>
      <v1:requestDetail>
        { xml }
      </v1:requestDetail>
    </v1:submitDeclarationRequest>
}
