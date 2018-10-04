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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.model.FileTransmission
import util.CustomsDeclarationsExternalServicesConfig._
import util.WireMockRunner

trait FileTransmissionService extends WireMockRunner {
  private val urlMatchingRequestPath = urlMatching(FileTransmissionContext)

  def startFileTransmissionService(): Unit = {
    setupFileTransmissionToReturn(NO_CONTENT)
  }

  def setupFileTransmissionToReturn(status: Int): StubMapping = stubFor(post(urlMatchingRequestPath).
    willReturn(
      aResponse()
        .withStatus(status)))

  def verifyFileTransmissionServiceWasCalledWith(request: FileTransmission) {
    verify(
      1,
      postRequestedFor(urlMatchingRequestPath)
      .withHeader(USER_AGENT, equalTo("customs-declarations"))
      .withHeader(ACCEPT, equalTo(JSON))
      .withRequestBody(equalToJson(Json.toJson(request).toString))
    )
  }

  def verifyFileTransmissionServiceWasCalled() {
    verify(
      1,
      postRequestedFor(urlMatchingRequestPath)
      .withHeader(USER_AGENT, equalTo("customs-declarations"))
      .withHeader(ACCEPT, equalTo(JSON))
    )
  }
}
