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

import scala.annotation.tailrec
import scala.xml._

@Singleton
class StatusResponseFilterService @Inject() (declarationsLogger: DeclarationsLogger,
                                             declarationsConfigService: DeclarationsConfigService) {

  private val namespacePrefix = "stat"
  private val namespaceBinding = NamespaceBinding(namespacePrefix, "http://gov.uk/customs/declarations/status-request", TopScope)
  private val minimizeEmptyElement = false

  def transform(xml: NodeSeq): NodeSeq = {

    var declaration = Elem(namespacePrefix, "declaration", Null, namespaceBinding, minimizeEmptyElement)
    val path: NodeSeq = xml \ "responseDetail" \ "declarationManagementInformationResponse" \ "declaration"

    val labels = Seq("versionNumber", "creationDate", "goodsItemCount", "tradeMovementType", "type", "packageCount", "acceptanceDate")
    declaration = maybeAddNode(path, declaration, labels)

    (path \ "parties").foreach { parties =>
      val numberNode = parties \ "partyIdentification" \ "number"
      if (numberNode.isEmpty) {
        val partiesElement = Elem(namespacePrefix, "parties", Null, namespaceBinding, minimizeEmptyElement)
        declaration = addChild(declaration, partiesElement)
      } else {
        val responseNode = buildPartyIdentificationNumberElement(parties, numberNode)
        declaration = addChild(declaration, responseNode)
      }
    }

    val root = Elem(namespacePrefix, "declarationManagementInformationResponse", Null, namespaceBinding, minimizeEmptyElement, declaration)
    declarationsLogger.debugWithoutRequestContext(s"created status response xml ${root.toString()}")
    root
  }

  @tailrec
  private def maybeAddNode(path: NodeSeq, declaration: Elem, labels: Seq[String]): Elem = {

    if (labels.isEmpty) {
      declaration
    } else {
      val inputNode = path \ labels.head
      val decChild = if (inputNode.nonEmpty) {
        addNode(inputNode.head, declaration)
      } else {
        declaration
      }
      maybeAddNode(path, decChild, labels.tail)
    }
  }

  private def addNode(node: Node, dec: Elem): Elem = {
    val responseNode = Elem(namespacePrefix, node.label, node.attributes, namespaceBinding, minimizeEmptyElement, Text(node.text))
    addChild(dec, responseNode)
  }

  private def buildPartyIdentificationNumberElement(parties: NodeSeq, numberNode: NodeSeq) = {
    val partyNumber = Elem(namespacePrefix, "number", Null, namespaceBinding, minimizeEmptyElement, Text(numberNode.head.text))
    val partyIdentification = Elem(namespacePrefix, "partyIdentification", Null, namespaceBinding, minimizeEmptyElement, partyNumber)
    Elem(namespacePrefix, "parties", Null, namespaceBinding, minimizeEmptyElement, partyIdentification)
  }

  private def addChild(n: Node, newChild: Node): Elem = n match {
    case Elem(prefix, label, attributes, scope, child @ _*) =>
      Elem(prefix, label, attributes, scope, minimizeEmptyElement, child ++ newChild : _*)
    case _ => throw new IllegalStateException("unable to add child node")
  }

}
