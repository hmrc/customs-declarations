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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.http.ws._

@Singleton
class NoAuditHttpClient @Inject()( config: Configuration,
                                   override val wsClient: WSClient,
                                   override val actorSystem: ActorSystem) extends HttpClient with WSHttp {
  override lazy val configuration: Option[Config] = Option(config.underlying)
  override val hooks: Seq[HttpHook] = Seq.empty
}
