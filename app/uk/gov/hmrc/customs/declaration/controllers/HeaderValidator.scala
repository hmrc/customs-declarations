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
import play.api.mvc.{ActionBuilder, Request, Result, Results}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorAcceptHeaderInvalid, ErrorContentTypeHeaderInvalid}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.RequestedVersionService

import scala.concurrent.Future

trait HeaderValidator extends Results {

  def declarationsLogger: DeclarationsLogger
  def requestedVersionService: RequestedVersionService

  type Validation = Option[String] => Boolean

  private lazy val validAcceptHeaders = requestedVersionService.validAcceptHeaders

  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML, MimeTypes.XML + "; charset=utf-8")

  val acceptHeaderValidation: Validation = _ exists validAcceptHeaders.contains
  val contentTypeValidation: Validation = _ exists (header => validContentTypeHeaders.contains(header.toLowerCase))

  def validateAccept(rules: Validation): ActionBuilder[Request] =
    validateHeader(rules, HeaderNames.ACCEPT, ErrorAcceptHeaderInvalid.XmlResult)

  def validateContentType(rules: Validation): ActionBuilder[Request] =
    validateHeader(rules, HeaderNames.CONTENT_TYPE, ErrorContentTypeHeaderInvalid.XmlResult)

  private def validateHeader(rules: Validation, headerName: String, error: => Result): ActionBuilder[Request] = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      val headerIsValid = rules(request.headers.get(headerName))
      getResponse(request, block, headerIsValid, error)
    }
  }

  private def getResponse[A](request: Request[A], block: (Request[A]) => Future[Result], headerIsValid: Boolean, error: => Result) = {
    if (headerIsValid) {
      block(request)
    } else {
      //TODO MC should be specific which header actually was not as expected. Plus log with Conversation and client ID, and return conversation id.
      declarationsLogger.errorWithoutHeaderCarrier(s"Invalid headers: ${request.headers.headers}")
      Future.successful(error)
    }
  }

}
