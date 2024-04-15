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

package uk.gov.hmrc.customs.declaration.controllers.dynamicservice

import com.google.inject.Inject
import play.api.libs.json.{JsError, JsSuccess, Json, OWrites, Reads}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.declaration.config.{InvalidEnvironmentException, ServiceConfigProvider}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

class DynamicServiceConfigurationController @Inject()(serviceConfigProvider: ServiceConfigProvider,
                                                      cc: ControllerComponents)
  extends BackendController(cc) {

  private implicit val rds: Reads[ServiceConfigDto] = Json.reads[ServiceConfigDto]
  private implicit val wrt: OWrites[ViewServiceConfigDto] = Json.writes[ViewServiceConfigDto]

  def setConfigurationForService(service: String): Action[AnyContent] = Action { request =>
    request.body.asJson.get.validate[ServiceConfigDto] match {
      case success: JsSuccess[ServiceConfigDto] =>
        val dto = success.value
        setEnvironmentForService(service, dto.environment)
      case _: JsError => BadRequest("no environment was provided")
    }
  }

  def getConfigurationForService(service: String): Action[AnyContent] = Action { request =>
    try {
      val config = serviceConfigProvider.getConfig(service)
      val body = ViewServiceConfigDto(service, config.environment, config.url, config.bearerToken.getOrElse(""))
      Ok(Json.toJson(body)).as(JSON)
    } catch {
      case _: RuntimeException => NotFound(s"Configuration was not found for $service")
    }
  }

  private def setEnvironmentForService(service: String, environment: String) = {
    try {
      serviceConfigProvider.setEnvironment(service, environment)
      Ok(s"The service $service is now configured to use the $environment environment")
    }
    catch {
      case _: InvalidEnvironmentException => NotFound(s"No configuration was found for service $service in environment $environment")
      case e: Throwable => throw e
    }
  }
}

case class ServiceConfigDto(environment: String)

case class ViewServiceConfigDto(service: String, environment: String, url: String, bearerToken: String)
