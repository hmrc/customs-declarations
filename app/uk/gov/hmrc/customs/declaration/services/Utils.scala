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
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_ACCEPTABLE, NOT_FOUND, UNAUTHORIZED, UNSUPPORTED_MEDIA_TYPE}

object Utils {
  def errorResponseForErrorCode(errorCode: Int): ErrorResponse = errorCode match {
    case BAD_REQUEST => ErrorResponse.ErrorGenericBadRequest
    case UNAUTHORIZED => ErrorResponse.ErrorUnauthorized
    case FORBIDDEN => ErrorResponse.ErrorPayloadForbidden
    case NOT_FOUND => ErrorResponse.ErrorNotFound
    case NOT_ACCEPTABLE => ErrorResponse.ErrorInvalidPayload
    case UNSUPPORTED_MEDIA_TYPE => ErrorResponse.ErrorContentTypeHeaderInvalid
    case _ => ErrorResponse.ErrorInternalServerError
  }
}
