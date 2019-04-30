/*
 * Copyright 2019 HM Revenue & Customs
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

import scala.xml._

@Singleton
class StatusResponseFilterService @Inject() (declarationsLogger: DeclarationsLogger,
                                             declarationsConfigService: DeclarationsConfigService) {

  private val newLineAndIndentation = Text("\n        ")

  def transform(xml: NodeSeq): NodeSeq = {
    val maybeAcceptanceDateTime = extract(xml, buildPath(xml, "acceptanceDate"))
    val maybeVersionId = extract(xml, buildPath(xml, "versionNumber"))
    val maybeCreationDateTime = extract(xml, buildPath(xml, "receiveDate"))
    val maybeTradeMovementType = extract(xml, buildPath(xml, "tradeMovementType"))
    val maybeType = extract(xml, buildPath(xml, "type"))
    val maybeTypeCode = (maybeTradeMovementType ++ maybeType).reduceOption(_ + _)
    val maybeGoodsItemQuantity = extract(xml, buildPath(xml, "goodsItemCount"))
    val maybeTotalPackageQuantity = extract(xml, buildPath(xml, "packageCount"))
    val maybeSubmitterId = extractSubmitterId(xml)

    val response = <v1:DeclarationStatusResponse
      xmlns:v1="http://gov.uk/customs/declarationInformationRetrieval/status/v1"
      xmlns:_2="urn:wco:datamodel:WCO:DEC-DMS:2"
      xmlns:_3="urn:wco:datamodel:WCO:Response_DS:DMS:2"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v1 ../Schemas/declarationInformationRetrievalStatusResponse-2.xsd ">
      <v1:Declaration>{maybeAcceptanceDateTime.fold(NodeSeq.Empty)(acceptanceDateTime => Seq[Node](newLineAndIndentation,
        <v1:AcceptanceDateTime>
          <_3:DateTimeString formatCode="304">{acceptanceDateTime}</_3:DateTimeString>
        </v1:AcceptanceDateTime>
      ))}{maybeVersionId.fold(NodeSeq.Empty)(versionId => Seq[Node](newLineAndIndentation,
        <v1:VersionID>{versionId}</v1:VersionID>))}{maybeCreationDateTime.fold(NodeSeq.Empty)(creationDateTime => Seq[Node](newLineAndIndentation,
        <v1:CreationDateTime>
          <v1:DateTimeString formatCode="304">{creationDateTime}</v1:DateTimeString>
        </v1:CreationDateTime>
      ))}</v1:Declaration>
      <_2:Declaration>
        <_2:FunctionCode>9</_2:FunctionCode>{maybeTypeCode.fold(NodeSeq.Empty)(typeCode => Seq[Node](newLineAndIndentation,
        <_2:TypeCode>{typeCode}</_2:TypeCode>))}{maybeGoodsItemQuantity.fold(NodeSeq.Empty)(goodItemQuantity => Seq[Node](newLineAndIndentation,
        <_2:GoodsItemQuantity unitCode="NPR">{goodItemQuantity}</_2:GoodsItemQuantity>))}{maybeTotalPackageQuantity.fold(NodeSeq.Empty)(totalPackageQuantity => Seq[Node](newLineAndIndentation,
        <_2:TotalPackageQuantity>{totalPackageQuantity}</_2:TotalPackageQuantity>))}{maybeSubmitterId.fold(NodeSeq.Empty)(submitterId => Seq[Node](newLineAndIndentation,
        <_2:Submitter>
          <_2:ID>{submitterId}</_2:ID>
        </_2:Submitter>
      ))}
      </_2:Declaration>
    </v1:DeclarationStatusResponse>

    declarationsLogger.debugWithoutRequestContext(s"created status response xml ${response.toString()}")
    response
  }

  private def extractSubmitterId(sourceXml: NodeSeq): Option[String] = {
    val tbParty = buildPath(sourceXml,"parties").filter{ party => (party \ "type").text == "TB" }

    if (tbParty.nonEmpty && (tbParty \ "partyIdentification" \ "number").head.nonEmpty) {//TODO remove nonEmpty check?
      val id = (tbParty \ "partyIdentification" \ "number").head
      if (id.nonEmpty) {
        Some(id.text)
      } else {
        None
      }
    } else {
      None
    }
  }

  private def buildPath(sourceXml: NodeSeq, label: String) = {
    sourceXml \ "responseDetail" \ "declarationStatusResponse" \ "declaration" \ label
  }

  private def extract(sourceXml: NodeSeq, path: NodeSeq): Option[String] = {
    val node: Node = path.head

    if (node.nonEmpty) {
      Some(node.text)
    }
    else {
      None
    }
  }

}
