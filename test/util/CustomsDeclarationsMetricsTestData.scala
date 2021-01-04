/*
 * Copyright 2021 HM Revenue & Customs
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

package util

import java.time.{ZoneId, ZonedDateTime}

import uk.gov.hmrc.customs.declaration.model.CustomsDeclarationsMetricsRequest

object CustomsDeclarationsMetricsTestData {

  val UtcZoneId: ZoneId = ZoneId.of("UTC")
  val EventStart: ZonedDateTime = ZonedDateTime.of(2015, 11, 30, 23, 45,
    59, 0, UtcZoneId)
  val EventEnd: ZonedDateTime = EventStart.plusSeconds(2)

  val ValidCustomsDeclarationsMetricsRequest: CustomsDeclarationsMetricsRequest =
    CustomsDeclarationsMetricsRequest("DECLARATION", TestData.conversationId, EventStart, EventEnd)

}
