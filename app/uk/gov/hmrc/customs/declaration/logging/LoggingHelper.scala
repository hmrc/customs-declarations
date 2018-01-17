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

import play.api.http.HeaderNames.AUTHORIZATION
import uk.gov.hmrc.customs.declaration.model.{Ids, MaybeIds, SeqOfHeader}
import uk.gov.hmrc.http.HeaderCarrier

object LoggingHelper {

  private val headerOverwriteValue = "value-not-logged"
  private val headersToOverwrite = Set(AUTHORIZATION)

  def formatError(msg: String, maybeIds: Option[Ids])(implicit hc: HeaderCarrier): String = {
    formatInfo(msg, maybeIds)
  }

  def formatWarn(msg: String)(implicit hc: HeaderCarrier): String = {
    formatInfo(msg, None)
  }

  def formatInfo(msg: String, maybeIds: MaybeIds = None)(implicit hc: HeaderCarrier): String = {
    val headers = hc.headers
    s"${format(headers, maybeIds)} $msg".trim
  }

  def formatInfo(msg: String, headers: SeqOfHeader): String = {
    s"${format(headers, maybeIds = None)} $msg".trim
  }

  def formatDebug(msg: String, ids: Ids)(implicit hc: HeaderCarrier): String = {
    formatDebug(msg, hc.headers, ids)
  }

  def formatDebug(msg: String, headers: SeqOfHeader, ids: Ids): String = {
    s"${format(headers, maybeIds = None)} $msg\nrequest headers=${overwriteHeaderValues(headers, headersToOverwrite - AUTHORIZATION)} ".trim
  }

  private def format(headers: SeqOfHeader, maybeIds: MaybeIds = None): String = {
    lazy val maybeClientId: Option[String] = findHeaderValue("X-Client-ID", headers)
    lazy val maybeSubscriptionIdFromHeader: Option[String] = findHeaderValue("api-subscription-fields-id", headers)
    lazy val maybeSubscriptionIdFromHeaderOrIds = maybeSubscriptionIdFromHeader.orElse(maybeIds.flatMap(ids => ids.maybeClientSubscriptionId.map(_.value)))

    maybeClientId.fold("")(appId => s"[clientId=$appId]") +
      maybeSubscriptionIdFromHeaderOrIds.fold("")(fieldsId => s"[fieldsId=$fieldsId]") +
      maybeIds.fold("") { ids =>
        lazy val maybeRequestedVersion = ids.maybeRequestedVersion
        s"[conversationId=${ids.conversationId.value}]" +
          maybeRequestedVersion.fold("")(ver => s"[requestedApiVersion=${ver.versionNumber}]")
      }
  }

  private def findHeaderValue(headerName: String, headers: SeqOfHeader): Option[String] = {
    headers.collectFirst{
      case (`headerName`, headerValue) => headerValue
    }
  }

  private def overwriteHeaderValues(headers: SeqOfHeader, overwrittenHeaderNames: Set[String]): SeqOfHeader = {
    headers map {
      case (rewriteHeader, _) if overwrittenHeaderNames.contains(rewriteHeader) => rewriteHeader -> headerOverwriteValue
      case header => header
    }
  }
}
