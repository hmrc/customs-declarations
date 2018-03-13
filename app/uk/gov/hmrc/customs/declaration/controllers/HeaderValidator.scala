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

package uk.gov.hmrc.customs.declaration.controllers

import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.{Request, Results}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorAcceptHeaderInvalid, ErrorContentTypeHeaderInvalid, errorBadRequest}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.RequestedVersionService

trait HeaderValidator extends Results {

  def declarationsLogger: DeclarationsLogger

  def requestedVersionService: RequestedVersionService

  private lazy val validAcceptHeaders = requestedVersionService.validAcceptHeaders.toSeq

  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML, MimeTypes.XML + "; charset=utf-8")

  private lazy val ErrorResponseBadgeIdentifierHeaderMissing = errorBadRequest("X-Badge-Identifier header is missing or invalid")

  private type Validation = Option[String] => Boolean

  private val acceptHeaderValidation: Validation = _ exists validAcceptHeaders.contains
  private val contentTypeValidation: Validation = _ exists (header => validContentTypeHeaders.contains(header.toLowerCase))
  private val badgeIdentifierValidation: Validation = _.fold(true)(header => "^[0-9A-Z]{6,12}$".r.findFirstIn(header).isDefined)

  def validateAccept[A]()(implicit request: Request[A]): Option[ErrorResponse] = {
    validateHeader(acceptHeaderValidation, HeaderNames.ACCEPT, ErrorAcceptHeaderInvalid)
  }

  def validateContentType[A]()(implicit request: Request[A]): Option[ErrorResponse] =
    validateHeader(contentTypeValidation, HeaderNames.CONTENT_TYPE, ErrorContentTypeHeaderInvalid)

  def validateBadgeIdentifier[A]()(implicit request: Request[A]): Option[ErrorResponse] =
    validateHeader(badgeIdentifierValidation, "X-Badge-Identifier", ErrorResponseBadgeIdentifierHeaderMissing)

  private def validateHeader[A](rules: Validation, headerName: String, error: => ErrorResponse)(implicit request: Request[A]): Option[ErrorResponse] = {
    if (rules(request.headers.get(headerName))) {
      None
    }
    else {
      Some(error)
    }

  }


}
