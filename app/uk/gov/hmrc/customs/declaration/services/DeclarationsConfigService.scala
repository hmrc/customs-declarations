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

package uk.gov.hmrc.customs.declaration.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.customs.api.common.config.ConfigValidationNelAdaptor
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.DeclarationsConfig

import scalaz.syntax.apply._
import scalaz.syntax.traverse._

@Singleton
class DeclarationsConfigService @Inject()(configValidationNel: ConfigValidationNelAdaptor, logger: DeclarationsLogger) extends DeclarationsConfig {

  private val customsNotificationsServiceNel = configValidationNel.service("customs-notification")
  private lazy val customsConfigHolder =
    (configValidationNel.service("api-subscription-fields").serviceUrl |@|
     customsNotificationsServiceNel.serviceUrl |@|
     customsNotificationsServiceNel.string("bearer-token"))(DeclarationsConfigImpl.apply) fold(
      fail = { nel =>
        // error case exposes nel (a NotEmptyList)
        val errorMsg = nel.toList.mkString("\n", "\n", "")
        logger.errorWithoutRequestContext(errorMsg)
        throw new IllegalStateException(errorMsg)
      },
      succ = identity
    )

  val apiSubscriptionFieldsBaseUrl: String = customsConfigHolder.apiSubscriptionFieldsBaseUrl

  val customsNotificationBaseBaseUrl: String = customsConfigHolder.customsNotificationBaseBaseUrl

  val customsNotificationBearerToken: String = customsConfigHolder.customsNotificationBearerToken

  private case class DeclarationsConfigImpl(
    apiSubscriptionFieldsBaseUrl: String,
    customsNotificationBaseBaseUrl: String,
    customsNotificationBearerToken: String
  ) extends DeclarationsConfig

}
