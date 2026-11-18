/*
 * Copyright 2026 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import util.CustomsDeclarationsExternalServicesConfig.*
import util.WireMockRunner

trait MdgDec64Service extends WireMockRunner {
  private val urlMatchingRequestPath = urlEqualTo(MdgDec64Context)

  def setupMdgDec64ToReturn(status: Int, body: String = ""): StubMapping =
    stubFor(post(urlMatchingRequestPath).willReturn(aResponse().withStatus(status).withBody(body)))

  def setupMdgDec64ToDelay(status: Int, delayMillis: Int): StubMapping =
    stubFor(post(urlMatchingRequestPath).willReturn(aResponse().withStatus(status).withFixedDelay(delayMillis)))

  def setupMdgDec64ToFault(fault: Fault): StubMapping =
    stubFor(post(urlMatchingRequestPath).willReturn(aResponse().withFault(fault)))

  def theRequestMadeToMdgDec64(): LoggedRequest = {
    verify(1, postRequestedFor(urlMatchingRequestPath))
    findAll(postRequestedFor(urlMatchingRequestPath)).get(0)
  }
}
