/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.http
import uk.gov.hmrc.http.HttpException

/**
  * Exception we raise for any HTTPResponse we receive that are not a 2xx statuses.
  *
  * Used only to maintain legacy code that previously relied upon http-verbs throwing
  * UpstreamErrorResponse exceptions for non 2xx statuses
  *
  * @param status that we received
  */
class Non2xxHttpResponse(status: Int) extends HttpException("Received a non 2XX response", status)