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

package uk.gov.hmrc.customs.declaration.model

import uk.gov.hmrc.customs.declaration.model.RequestType.RequestType

case class RequestedVersion(versionNumber: String, configPrefix: Option[String])

case class Eori(value: String) extends AnyVal

case class ConversationId(value: String) extends AnyVal

case class BadgeIdentifier(value: String) extends AnyVal

case class FieldsId(value: String) extends AnyVal

//TODO MC it will be revisited anyway while introducing ActionBuilder solution
//renaming would introduce a lot of noise, so will be done in separate PR
case class Ids(conversationId: ConversationId,
               requestType: RequestType,
               maybeClientSubscriptionId: Option[FieldsId] = None,
               maybeRequestedVersion: Option[RequestedVersion] = None,
               maybeBadgeIdentifier: Option[BadgeIdentifier] = None
              )

object RequestType extends Enumeration {
  type RequestType = Value
  val Submit, Cancel = Value
}
