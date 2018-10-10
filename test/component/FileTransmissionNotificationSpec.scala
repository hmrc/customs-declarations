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

package component

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, OptionValues}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION, CONTENT_TYPE, contentAsString, route, status, _}
import util.ApiSubscriptionFieldsTestData
import util.CustomsDeclarationsExternalServicesConfig.CustomsNotificationAuthHeaderValue
import util.FileTransmissionTestData._
import util.TestData.FileReferenceOne
import util.externalservices.CustomsNotificationService

import scala.xml.Utility.trim


class FileTransmissionNotificationSpec extends ComponentTestSpec with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ApiSubscriptionFieldsTestData
  with CustomsNotificationService
  with Eventually
  with IntegrationPatience {

  private val endpoint = s"/file-transmission-notify/clientSubscriptionId/$subscriptionFieldsIdString"

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("File Transmission Notification") {
    scenario("Success request has been made to Customs Notification service") {
      notificationServiceIsRunning()
      Given("the File Transmission service sends a notification")

      When("File Transmission service notifies Declaration API using previously provided callback URL")
      val validRequest = FakeRequest("POST", endpoint).withJsonBody(Json.parse(FileTransmissionSuccessNotificationPayload))
      val result = route(app = app, validRequest).value

      Then("Declaration Service returns 204")
      status(result) shouldBe 204

      And("the response body is empty")
      contentAsString(result) shouldBe empty

      And("a request is made to Custom Notification Service")
      val (requestHeaders, requestPayload) = eventually(aRequestWasMadeToNotificationService())

      And("The clientSubscriptionId is passed as X-CDS-Client-ID")
      requestHeaders.get("X-CDS-Client-ID") shouldBe Some(subscriptionFieldsIdString)

      And("The reference is passed as X-Conversation-ID")
      requestHeaders.get("X-Conversation-ID") shouldBe Some(FileReferenceOne.toString)

      And("The Authorization header contains the value which is configured in the configs")
      requestHeaders.get(AUTHORIZATION) shouldBe Some(s"Basic $CustomsNotificationAuthHeaderValue")

      And("The Content-Type header is application/xml; charset=UTF-8")
      requestHeaders.get(CONTENT_TYPE) shouldBe Some("application/xml; charset=UTF-8")

      And("The Accept header is application/xml")
      requestHeaders.get(ACCEPT) shouldBe Some("application/xml")

      And("The request XML payload contains details of the scan outcome")
      trim(string2xml(requestPayload)) shouldBe trim(FileTransmissionSuccessCustomsNotificationXml)
    }

    scenario("Response status 400 when File Transmission service sends invalid payload") {
      Given("the File Transmission service sends a notification")

      When("File Transmission service notifies Declaration API using previously provided callback URL with invalid json payload")
      val invalidRequest = FakeRequest("POST", endpoint).withJsonBody(Json.parse(InvalidFileTransmissionNotificationPayload))
      val result = route(app = app, invalidRequest).value

      Then("a response with a 400 status is returned")
      status(result) should be (BAD_REQUEST)
    }
  }

}
