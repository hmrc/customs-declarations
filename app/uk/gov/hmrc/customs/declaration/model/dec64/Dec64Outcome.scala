/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.model.dec64

import play.api.libs.json.*
import uk.gov.hmrc.customs.declaration.model.upscan.{BatchId, FileReference}

sealed trait Dec64Outcome

object Dec64Outcome {
  private case object Delivered extends Dec64Outcome

  final case class Failed(reason: String) extends Dec64Outcome

  implicit val format: OFormat[Dec64Outcome] = OFormat[Dec64Outcome](
    (json: JsValue) => (json \ "status").validate[String].flatMap {
      case "DELIVERED" => JsSuccess(Delivered)
      case "FAILED" => (json \ "reason").validate[String].map(Failed(_))
      case other => JsError(s"unknown DEC64 outcome '$other'")
    },
    {
      case Delivered => Json.obj("status" -> "DELIVERED")
      case Failed(reason) => Json.obj("status" -> "FAILED", "reason" -> reason)
    }
  )
}

case class Dec64OutcomeNotification(fileReference: FileReference, batchId: BatchId, outcome: Dec64Outcome)
