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

import com.github.tomakehurst.wiremock.client.WireMock.{post, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.test.Helpers._
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.{TestData, WireMockRunner}

import scala.collection.JavaConverters._

trait CustomsNotificationService extends WireMockRunner {

  private val notifyPath = urlMatching("/customs-notification/notify")

  def notificationServiceIsRunning() {
    stubFor(post(notifyPath)
      .willReturn(
        aResponse()
          .withStatus(ACCEPTED)))
  }

  def setupCustomsNotificationToReturn(status: Int): StubMapping = stubFor(post(notifyPath).
    willReturn(
      aResponse()
        .withStatus(status)))

  def aRequestWasMadeToNotificationService(): (Map[String, String], String) = {
    verify(1, postRequestedFor(notifyPath))
    val req = findAll(postRequestedFor(notifyPath)).get(0)
    val keys: List[String] = List.concat(req.getHeaders.keys().asScala)
    (Map(keys map { s => (s, req.getHeader(s)) }: _*), req.getBodyAsString)
  }

  def verifyWasCalledWith(requestBody: String) {
    verify(
      1,
      postRequestedFor(notifyPath)
        .withHeader("X-CDS-Client-ID", equalTo(subscriptionFieldsId.toString))
        .withHeader("X-Conversation-ID", equalTo(TestData.conversationId.toString))
        .withHeader(CONTENT_TYPE, equalTo("application/xml; charset=UTF-8"))
        .withHeader(ACCEPT, equalTo("application/xml"))
        .withHeader(AUTHORIZATION, equalTo("Basic some-basic-auth"))
        .withRequestBody(equalToXml(requestBody))
    )
  }


  def noRequestWasMadeToNotificationService(): Unit = {
    verify(0, postRequestedFor(notifyPath))
  }
}
