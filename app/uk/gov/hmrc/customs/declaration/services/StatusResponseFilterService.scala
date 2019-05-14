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
    val maybeMrn = extract(xml, buildPath(xml, "reference"))
    val maybeVersionId = extract(xml, buildPath(xml, "versionNumber"))
    val maybeCreationDateTime = extract(xml, buildPath(xml, "receiveDate"))
    val maybeTradeMovementType = extract(xml, buildPath(xml, "tradeMovementType"))
    val maybeType = extract(xml, buildPath(xml, "type"))
    val maybeTypeCode = (maybeTradeMovementType._1 ++ maybeType._1).reduceOption(_ + _)
    val maybeGoodsItemQuantity: (Option[String], Option[MetaData]) = extract(xml, buildPath(xml, "goodsItemCount"))
    val maybeTotalPackageQuantity = extract(xml, buildPath(xml, "packageCount"))
    val maybeSubmitterId = extractSubmitterId(xml)

    <v1:DeclarationStatusResponse xmlns:v1="http://gov.uk/customs/declarationInformationRetrieval/status/v1"
                                                 xmlns:_2="urn:wco:datamodel:WCO:DEC-DMS:2"
                                                 xmlns:_3="urn:wco:datamodel:WCO:Response_DS:DMS:2"
                                                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                                 xsi:schemaLocation="http://gov.uk/customs/declarationInformationRetrieval/status/v1 ../Schemas/declarationInformationRetrievalStatusResponse.xsd">
      <v1:Declaration>{maybeAcceptanceDateTime._1.fold(NodeSeq.Empty)(acceptanceDateTime => Seq[Node](newLineAndIndentation,
        <v1:AcceptanceDateTime>
          <_3:DateTimeString formatCode={outputAttribute(maybeAcceptanceDateTime._2, "formatCode")}>{acceptanceDateTime}</_3:DateTimeString>
        </v1:AcceptanceDateTime>
      ))}{maybeVersionId._1.fold(NodeSeq.Empty)(versionId => Seq[Node](newLineAndIndentation,
        <v1:VersionID>{versionId}</v1:VersionID>))}{maybeMrn._1.fold(NodeSeq.Empty)(mrn => Seq[Node](newLineAndIndentation,
        <v1:ID>{mrn}</v1:ID>))}{maybeCreationDateTime._1.fold(NodeSeq.Empty)(creationDateTime => Seq[Node](newLineAndIndentation,
        <v1:CreationDateTime>
          <v1:DateTimeString formatCode={outputAttribute(maybeCreationDateTime._2, "formatCode")}>{creationDateTime}</v1:DateTimeString>
        </v1:CreationDateTime>
      ))}</v1:Declaration>
      <_2:Declaration>
        <_2:FunctionCode>9</_2:FunctionCode>{maybeTypeCode.fold(NodeSeq.Empty)(typeCode => Seq[Node](newLineAndIndentation,
        <_2:TypeCode>{typeCode}</_2:TypeCode>))}{maybeGoodsItemQuantity._1.fold(NodeSeq.Empty)(goodItemQuantity => Seq[Node](newLineAndIndentation,
        <_2:GoodsItemQuantity unitType={outputAttribute(maybeGoodsItemQuantity._2, "unitType")}>{goodItemQuantity}</_2:GoodsItemQuantity>))}{maybeTotalPackageQuantity._1.fold(NodeSeq.Empty)(totalPackageQuantity => Seq[Node](newLineAndIndentation,
        <_2:TotalPackageQuantity>{totalPackageQuantity}</_2:TotalPackageQuantity>))}{maybeSubmitterId.fold(NodeSeq.Empty)(submitterId => Seq[Node](newLineAndIndentation,
        <_2:Submitter>
          <_2:ID>{submitterId}</_2:ID>
        </_2:Submitter>
      ))}
      </_2:Declaration>
    </v1:DeclarationStatusResponse>
  }

  private def outputAttribute(maybeAttributes: Option[MetaData], attributeLabel: String): Option[Text] = {
    maybeAttributes.fold[Option[Text]](None){attr =>
      attr.get(attributeLabel).fold[Option[Text]](None){attrValue =>
        Some(Text(attrValue.text))
      }
    }
  }

  private def extractSubmitterId(sourceXml: NodeSeq): Option[String] = {
    val tbParty = buildPath(sourceXml,"parties").filter{ party => (party \ "type").text == "TB" }

    if (tbParty.nonEmpty && (tbParty \ "partyIdentification" \ "number").head.nonEmpty) {
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

  private def buildPath(sourceXml: NodeSeq, label: String): NodeSeq = {
    sourceXml \ "responseDetail" \ "declarationStatusResponse" \ "declaration" \ label
  }

  private def extract(sourceXml: NodeSeq, path: NodeSeq): (Option[String], Option[MetaData]) = {
    if (path.nonEmpty && path.head.nonEmpty) {
      (Some(path.head.text), Some(path.head.attributes))
    }
    else {
      (None, None)
    }
  }

}
