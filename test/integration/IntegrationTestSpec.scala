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

package integration

import com.google.inject.AbstractModule
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpec}
import play.api.inject.guice.GuiceableModule
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import util.UnitSpec

case class IntegrationTestModule(mockLogger: DeclarationsLogger) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DeclarationsLogger]) toInstance mockLogger
  }

  def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
}

trait IntegrationTestSpec extends WordSpec
  with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
