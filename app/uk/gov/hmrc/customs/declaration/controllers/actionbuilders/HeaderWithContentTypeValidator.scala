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

package uk.gov.hmrc.customs.declaration.controllers.actionbuilders

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes
import play.api.mvc.Headers
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders._

@Singleton
class HeaderWithContentTypeValidator @Inject()(logger: DeclarationsLogger) extends HeaderValidator(logger) {

  private lazy val validContentTypeHeaders = Seq(MimeTypes.XML, MimeTypes.XML + ";charset=utf-8", MimeTypes.XML + "; charset=utf-8")

  override def validateHeaders[A](implicit apiVersionRequest: ApiVersionRequest[A]): Either[ErrorResponse, ExtractedHeaders] = {

    implicit val headers: Headers = apiVersionRequest.headers

    def hasContentType = validateHeader(CONTENT_TYPE, s => validContentTypeHeaders.contains(s.toLowerCase()), ErrorContentTypeHeaderInvalid)

    super.validateHeaders match {
      case Right(b) =>
        val theResult: Either[ErrorResponse, ExtractedHeaders] = for {
          hasContentType <- hasContentType.right
        } yield {
          logger.debug(s"${logAcceptAndClientIdHeaderText(b.clientId)}" +
            s"\n$CONTENT_TYPE header passed validation: $hasContentType")
          b
        }
        theResult
      case Left(a) => Left(a)
    }
  }
}

