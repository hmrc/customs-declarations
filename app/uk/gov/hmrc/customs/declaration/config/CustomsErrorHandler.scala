/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.customs.declaration.config

import play.api.http.{DefaultHttpErrorHandler, MediaRange}
import play.api.mvc.{AcceptExtractors, RequestHeader, Result}
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.Future
import scala.util.matching.Regex

@Singleton
class CustomsErrorHandler @Inject()(environment: Environment, configuration: Configuration,
                                    sourceMapper: OptionalSourceMapper, router: Provider[Router])
  extends DefaultHttpErrorHandler(environment, configuration, sourceMapper, router) with AcceptExtractors {

  override protected def onBadRequest(request: RequestHeader, error: String): Future[Result] =
    Future.successful(resultFor(request, ErrorResponse.errorBadRequest(error)))

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] =
    Future.successful(resultFor(request, ErrorResponse.ErrorNotFound))

  override protected def onOtherClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    Future.successful(resultFor(request, ErrorResponse(statusCode, ErrorResponse.BadRequestCode, message)))

  override protected def onDevServerError(request: RequestHeader, e: UsefulException): Future[Result] =
    Future.successful(resultFor(request, ErrorResponse.errorInternalServerError(e.getMessage)))

  override protected def onProdServerError(request: RequestHeader, e: UsefulException): Future[Result] =
    Future.successful(resultFor(request, ErrorResponse.ErrorInternalServerError))


  private def resultFor(request: RequestHeader, errorResponse: ErrorResponse) =
    request match {
      case RequestAcceptsOnlyHmrcXml() | RequestAcceptsOnlyXml() => errorResponse.XmlResult
      case _ => errorResponse.JsonResult
    }

  private object RequestAcceptsOnlyHmrcXml extends AcceptsMediaRange("^application/vnd\\.hmrc\\..*\\+xml$".r)
  private object RequestAcceptsOnlyXml extends AcceptsMediaRange("^application/xml$".r)

  private class AcceptsMediaRange(val mediaRangeRegex: Regex) {
    def unapply(range: MediaRange): Boolean = mediaRangeRegex.findFirstIn(range.toString).isDefined

    def unapply(request: RequestHeader): Boolean = request.acceptedTypes match {
      case Seq(range) => unapply(range)
      case _ => false
    }
  }

}
