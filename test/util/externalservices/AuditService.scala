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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status.OK
import util.{CustomsDeclarationsExternalServicesConfig, WireMockRunner}

trait AuditService extends WireMockRunner {
  private val urlMatchingRequestPath = urlMatching(CustomsDeclarationsExternalServicesConfig.AuditContext)

  def setupAuditServiceToReturn(status: Int = OK): Unit =
    stubFor(post(urlMatchingRequestPath)
      willReturn aResponse()
      .withStatus(status))

  def verifyAuditServiceWasNotCalled(): Unit = {
    verify(0, postRequestedFor(urlMatchingRequestPath))
  }

}
