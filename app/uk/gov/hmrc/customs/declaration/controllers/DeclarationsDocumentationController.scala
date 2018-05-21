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

import javax.inject.{Inject, Singleton}

import play.api.Configuration
import play.api.http.HttpErrorHandler
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.customs.api.common.controllers.DocumentationController

@Singleton
class DeclarationsDocumentationController @Inject()(httpErrorHandler: HttpErrorHandler, configuration: Configuration) extends DocumentationController(httpErrorHandler) {

  private lazy val v1PrivateAccessEnabled = configuration.getBoolean("api.access.version-1.0.private").getOrElse(false)
  private lazy val v2PrivateAccessEnabled = configuration.getBoolean("api.access.version-2.0.private").getOrElse(false)

  private lazy val v1WhitelistedApplicationIds = configuration.getStringSeq("api.access.version-1.0.whitelistedApplicationIds").getOrElse(Seq.empty)
  private lazy val v2WhitelistedApplicationIds = configuration.getStringSeq("api.access.version-2.0.whitelistedApplicationIds").getOrElse(Seq.empty)

  private lazy val v2Enabled = configuration.getBoolean("api.access.version-2.0.enabled").getOrElse(true)

  def definition(): Action[AnyContent] = Action {
    Ok(uk.gov.hmrc.customs.declaration.views.txt.definition(
      v1PrivateAccessEnabled,
      v1WhitelistedApplicationIds,
      v2PrivateAccessEnabled,
      v2WhitelistedApplicationIds,
      v2Enabled)).withHeaders(CONTENT_TYPE -> JSON)
  }
}
