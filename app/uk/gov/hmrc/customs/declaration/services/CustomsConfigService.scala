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
import uk.gov.hmrc.customs.declaration.model.{ApiDefinitionConfig, CustomsEnrolmentConfig}

import scalaz.ValidationNel
import scalaz.syntax.apply._
import scalaz.syntax.traverse._

@Singleton
class CustomsConfigService @Inject()(configValidationNel: ConfigValidationNelAdaptor, logger: DeclarationsLogger) {

  private val root = configValidationNel.root

  private val validatedDefinitionConfig: ValidationNel[String, ApiDefinitionConfig] =
    root.string("customs.definition.api-scope").map(ApiDefinitionConfig.apply)

  private val validatedCustomsEnrolmentConfig: ValidationNel[String, CustomsEnrolmentConfig] = (
    root.string("customs.enrolment.name") |@|
      root.string("customs.enrolment.eori-identifier")
    ) (CustomsEnrolmentConfig.apply)

  private val customsConfigHolder =
    (validatedDefinitionConfig |@| validatedCustomsEnrolmentConfig) (CustomsConfigHolder.apply) fold(
      fail = { nel =>
        // error case exposes nel (a NotEmptyList)
        val errorMsg = nel.toList.mkString("\n", "\n", "")
        logger.errorWithoutHeaderCarrier(errorMsg)
        throw new IllegalStateException(errorMsg)
      },
      succ = identity
    )

  val apiDefinitionConfig: ApiDefinitionConfig = customsConfigHolder.apiDefinitionConfig

  val customsEnrolmentConfig: CustomsEnrolmentConfig = customsConfigHolder.customsEnrolmentConfig


  private case class CustomsConfigHolder(apiDefinitionConfig: ApiDefinitionConfig,
                                         customsEnrolmentConfig: CustomsEnrolmentConfig)

}
