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

package uk.gov.hmrc.customs.declaration.logging

import com.google.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Singleton

@Singleton
class CdsLogger @Inject()(serviceConfig: ServicesConfig) {

  private lazy val loggerName: String = serviceConfig.getString("application.logger.name")
  lazy val logger = play.api.Logger(loggerName)

  def debug(msg: => String): Unit = logger.debug(msg)
  def debug(msg: => String, e: => Throwable): Unit = logger.debug(msg, e)
  def info(msg: => String): Unit = logger.info(msg)
  def info(msg: => String, e: => Throwable): Unit = logger.info(msg, e)
  def warn(msg: => String): Unit = logger.warn(msg)
  def warn(msg: => String, e: => Throwable): Unit = logger.warn(msg, e)
  def error(msg: => String): Unit = logger.error(msg)
  def error(msg: => String, e: => Throwable): Unit = logger.error(msg, e)

}
