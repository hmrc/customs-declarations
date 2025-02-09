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

package integration

import com.google.inject.AbstractModule
import org.scalatest.concurrent.Eventually
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.inject.guice.GuiceableModule
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import util.UnitSpec

case class IntegrationTestModule(mockLogger: DeclarationsLogger) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[DeclarationsLogger]) `toInstance` mockLogger
  }

  def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
}

trait IntegrationTestSpec extends AnyWordSpecLike with UnitSpec
  with BeforeAndAfterEach with BeforeAndAfterAll with Eventually {

  /**
   * On Jenkins the localhost string is different to when run locally.
   * @return
   */
  def localhostString: String = {
    if (System.getenv("HOME") == "/home/jenkins") "127.0.0.1" else "[0:0:0:0:0:0:0:1]"
  }
}
