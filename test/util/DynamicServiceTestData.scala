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

package util

import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import uk.gov.hmrc.play.audit.AuditModule
import uk.gov.hmrc.play.bootstrap.backend.BackendModule

trait DynamicServiceTestData {

  protected val validService = "valid-service"
  protected val validServiceHost = "default-host"
  protected val validServicePort = 1111
  protected val validServiceContext = "/default-context"
  protected val validServiceBearer = "default-bearer"

  protected val validEnvironment = "valid-environment"
  protected val validEnvironmentHost = "env-host"
  protected val validEnvironmentPort = 2222
  protected val validEnvironmentContext = "/env-context"
  protected val validEnvironmentBearer = "env-bearer"

  protected val noBearerService = "no-bearer-service"
  protected val noBearerServiceHost = "no-bearer-host"
  protected val noBearerServicePort = 3333
  protected val noBearerServiceContext = "/no-bearer-context"

  protected val defaultEnvironment = "default"

  val configuredApplication = GuiceApplicationBuilder(
    modules = Seq(GuiceableModule.guiceable(new AuditModule), GuiceableModule.guiceable(new BackendModule))).
    configure(
      Map(
        "play.http.router" -> "dynamicservice.Routes",
        "appName" -> "customs-declarations",
        s"microservice.services.$validService.host" -> validServiceHost,
        s"microservice.services.$validService.port" -> validServicePort,
        s"microservice.services.$validService.context" -> validServiceContext,
        s"microservice.services.$validService.bearer-token" -> validServiceBearer,
        s"microservice.services.$validService.$validEnvironment.host" -> validEnvironmentHost,
        s"microservice.services.$validService.$validEnvironment.port" -> validEnvironmentPort,
        s"microservice.services.$validService.$validEnvironment.context" -> validEnvironmentContext,
        s"microservice.services.$validService.$validEnvironment.bearer-token" -> validEnvironmentBearer,
        s"microservice.services.$noBearerService.host" -> noBearerServiceHost,
        s"microservice.services.$noBearerService.port" -> noBearerServicePort,
        s"microservice.services.$noBearerService.context" -> noBearerServiceContext
      )).build()
}
