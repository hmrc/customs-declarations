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

package unit.controllers

import org.mockito.Mockito._
import org.scalatest.{Matchers}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declaration.controllers.TestOnlyController
import uk.gov.hmrc.customs.declaration.services.TestOnlyService

class TestOnlyControllerSpec extends AnyWordSpecLike
  with MockitoSugar with Matchers{

  private val mockTestOnlyService = mock[TestOnlyService]

  private def controller() = new TestOnlyController(mockTestOnlyService, Helpers.stubControllerComponents())

  "TestOnlyController" should {

    "respond with status 200 for valid request" in {
      val result = controller().deleteAll().apply(FakeRequest())
      verify(mockTestOnlyService).deleteAll()
      status(result) shouldBe OK
    }
  }

}
