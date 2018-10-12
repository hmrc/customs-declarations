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
import scala.collection.JavaConverters._

trait FileTransmissionService extends WireMockRunner {
  private val urlMatchingRequestPath = urlMatching(FileTransmissionContext)

  def startFileTransmissionService(): Unit = {
    setupFileTransmissionToReturn(NO_CONTENT)
  }

  def setupFileTransmissionToReturn(status: Int): StubMapping = stubFor(post(urlMatchingRequestPath).
    willReturn(
      aResponse()
        .withStatus(status)))

  def aRequestWasMadeToFileTransmissionService(): (Map[String, String], String) = {
    verify(1, postRequestedFor(urlMatchingRequestPath))
    val req = findAll(postRequestedFor(urlMatchingRequestPath)).get(0)
    val keys: List[String] = List.concat(req.getHeaders.keys().asScala)
    (Map(keys map { s => (s, req.getHeader(s)) }: _*), req.getBodyAsString)
  }

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
