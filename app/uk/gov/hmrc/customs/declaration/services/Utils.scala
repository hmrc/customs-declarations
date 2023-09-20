/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, UNAUTHORIZED}

object Utils {
  def errorResponseForErrorCode(errorCode: Int): ErrorResponse = {
    Map(
      BAD_REQUEST -> ErrorResponse.ErrorGenericBadRequest,
      UNAUTHORIZED -> ErrorResponse.ErrorUnauthorized,
      FORBIDDEN -> ErrorResponse.ErrorPayloadForbidden,
      NOT_FOUND -> ErrorResponse.ErrorNotFound
    ).getOrElse(errorCode, ErrorResponse.ErrorInternalServerError)
  }
}
