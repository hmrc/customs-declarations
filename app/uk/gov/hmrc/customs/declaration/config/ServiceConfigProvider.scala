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

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class ServiceConfigProvider @Inject()(servicesConfig: ServicesConfig) {

  val default = "default"

  private val serviceOverrides = scala.collection.mutable.Map[String, String]()

  def setEnvironment(serviceName: String, environment: String): Unit = {
    if(environment == default) {
      serviceOverrides.remove(serviceName)
    } else {
      overrideServiceWithEnvironment(serviceName, environment)
    }
  }

  private def overrideServiceWithEnvironment(serviceName: String, environment: String) = {
    try {
      servicesConfig.baseUrl(serviceName + "." + environment)
      serviceOverrides.put(serviceName, environment)
    } catch {
      case _: RuntimeException => throw new InvalidEnvironmentException(s"No configuration was found for service $serviceName in environment $environment")
    }
  }

  def getConfig(serviceName: String): ServiceConfig = {
    val config = serviceOverrides.get(serviceName)
    val serviceKey = config.fold(s"$serviceName")(env => s"$serviceName.$env")
    val env = config.fold(default)(env => env)

    val baseUrl = servicesConfig.baseUrl(serviceKey)
    val context = servicesConfig.getConfString(s"$serviceKey.context", throw new InvalidEnvironmentException(s"Context missing for service $serviceName in $env environment"))
    val bearer = servicesConfig.getConfString(s"$serviceKey.bearer-token", "")
    val maybeBearer = if (bearer == "") None else Some(bearer)

    ServiceConfig(s"$baseUrl$context", maybeBearer, env)
  }

  def reset(): Unit = {
    serviceOverrides.clear()
  }
}

case class ServiceConfig(url: String, bearerToken: Option[String], environment: String)

class InvalidEnvironmentException(message: String = "") extends RuntimeException(message)
