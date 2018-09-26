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

package uk.gov.hmrc.customs.declaration.logging

import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.mvc.Request
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames._
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ExtractedHeaders, HasAuthorisedAs, HasConversationId}

object LoggingHelper {
  private val headerSet = Set(CONTENT_TYPE.toLowerCase, ACCEPT.toLowerCase, XConversationIdHeaderName.toLowerCase, XClientIdHeaderName.toLowerCase, XBadgeIdentifierHeaderName.toLowerCase)

  def formatError(msg: String, r: HasConversationId): String = {
    formatMessage(msg, r)
  }

  def formatWarn(msg: String, r: HasConversationId): String = {
    formatMessage(msg, r)
  }

  def formatInfo(msg: String, r: HasConversationId): String = {
    formatMessage(msg, r)
  }

  def formatDebug(msg: String, r: HasConversationId): String = {
    formatMessage(msg, r)
  }

  def formatDebugFull(msg: String, r: HasConversationId with Request[_]): String = {
    formatMessageFull(msg, r)
  }

  private def formatMessage(msg: String, r: HasConversationId): String = {
    s"${format(r)} $msg".trim
  }

  private def format(r: HasConversationId): String = {
    def conversationId = s"[conversationId=${r.conversationId}]"

    def extractedHeaders = r match {
      case h: ExtractedHeaders => s"[clientId=${h.clientId}][requestedApiVersion=${h.requestedApiVersion}]"
      case _ => ""
    }
    def authorised = r match {
      case a: HasAuthorisedAs =>
        a.authorisedAs match {
          case Csp(badgeIdentifier, _) => s"[authorisedAs=Csp($badgeIdentifier)]"
          case NonCsp(eori, _) => s"[authorisedAs=NonCsp($eori)]"
          case BatchFileUploadCsp(eori, badgeIdentifier, _) => s"[authorisedAs=BatchFileUploadCsp($eori, $badgeIdentifier)]"
        }
      case _ => ""
    }
    s"$conversationId$extractedHeaders$authorised"
  }

  def formatMessageFull(msg: String, r: HasConversationId with Request[_]): String = {
    def filteredHeaders = r.headers.toSimpleMap.filter(keyValTuple =>
      headerSet.contains(keyValTuple._1.toLowerCase))

    s"[conversationId=${r.conversationId.uuid}] $msg headers=$filteredHeaders"
  }


}
