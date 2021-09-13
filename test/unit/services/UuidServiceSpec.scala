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

package unit.services

import org.scalatest.{Matchers}
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.customs.declaration.services.UuidService

class UuidServiceSpec extends AnyWordSpecLike with Matchers {

  private val uuidService = new UuidService

  "UuidService" should {
    "provide different value on each call" in {
      val uuid1 = uuidService.uuid()
      val uuid2 = uuidService.uuid()

      uuid1 should not be uuid2
      uuid1.toString should not be uuid2.toString
    }
  }

}
