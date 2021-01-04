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

package unit.connectors

import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsPath
import util.UnitSpec
import util.ApiSubscriptionFieldsTestData

class ApiSubscriptionFieldsPathSpec extends UnitSpec with ApiSubscriptionFieldsTestData {

  "ApiSubscriptionFieldsPath" should {
    "construct path" in {
      ApiSubscriptionFieldsPath.url("/some-context", apiSubscriptionKey) shouldBe "/some-context/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0"
    }
  }
}
