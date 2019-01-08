/*
 * Copyright 2019 HM Revenue & Customs
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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import util.WireMockRunner

trait RegistrationService extends WireMockRunner {

  private val RegistrationPath = urlMatching("/registration")

  def registrationServiceIsRunning() {
    stubFor(post(RegistrationPath).
      withHeader(CONTENT_TYPE, equalTo(JSON)).
      willReturn(
        aResponse()
          .withStatus(NO_CONTENT)))
  }

  def verifyRegistrationServiceWasCalledFor(json: JsValue) {
    verify(1, postRequestedFor(RegistrationPath)
      .withHeader(CONTENT_TYPE, equalTo(JSON))
      .withRequestBody(equalTo(json.toString)))
  }

  def noRequestWasMadeToRegistrationService() {
    verify(0, postRequestedFor(RegistrationPath))
  }
}
