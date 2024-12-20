/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest

import java.time.Instant
import scala.xml.{NodeSeq, Text}

class MdgPayloadDecorator() {

  private val newLineAndIndentation = "\n        "

  def wrap[A](xml: NodeSeq, sfId: ApiSubscriptionFieldsResponse, dateTime: Instant)(implicit vpr: ValidatedPayloadRequest[A]): NodeSeq =
    <v1:submitDeclarationRequest
    xmlns:v1="http://uk/gov/hmrc/mdg/declarationmanagement/submitdeclaration/request/schema/v1"
    xmlns:n1="urn:wco:datamodel:WCO:DEC-DMS:2"
    xmlns:p1="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <v1:requestCommon>
        <!--type: regimeType-->
        <v1:regime>CDS</v1:regime>
        <v1:receiptDate>{ dateTime }</v1:receiptDate>
        <v1:clientID>{sfId.fieldsId}</v1:clientID>
        <v1:conversationID>{vpr.conversationId.uuid}</v1:conversationID>
        {val as = vpr.authorisedAs

      as match {
            case NonCsp(eori, _) =>
              <v1:authenticatedPartyID>{eori.value}</v1:authenticatedPartyID> // originatingPartyID is only required for CSPs
            case Csp(_, badgeId, _) =>
              val badgeIdentifierElement: NodeSeq = {badgeId.fold(NodeSeq.Empty)(badge => <v1:badgeIdentifier>{badge.toString}</v1:badgeIdentifier>)}
              Seq[NodeSeq](
              badgeIdentifierElement, Text(newLineAndIndentation),
              <v1:originatingPartyID>{Csp.originatingPartyId(as.asInstanceOf[Csp])}</v1:originatingPartyID>, Text(newLineAndIndentation),
              <v1:authenticatedPartyID>{sfId.fields.authenticatedEori.get}</v1:authenticatedPartyID>)
          }
        }
      </v1:requestCommon>
      <v1:requestDetail>
        { xml }
      </v1:requestDetail>
    </v1:submitDeclarationRequest>

}
