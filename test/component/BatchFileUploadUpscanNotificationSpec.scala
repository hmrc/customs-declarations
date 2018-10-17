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
import play.api.test.Helpers.{ACCEPT, CONTENT_TYPE, contentAsString, route, status, _}
import uk.gov.hmrc.customs.declaration.model.ConversationId
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.repo.BatchFileUploadMetadataMongoRepo
import util.ApiSubscriptionFieldsTestData
import util.CustomsDeclarationsExternalServicesConfig.CustomsNotificationAuthHeaderValue
import util.TestData._
import util.UpscanNotifyTestData._
import util.externalservices.{CustomsNotificationService, FileTransmissionService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Utility.trim

class BatchFileUploadUpscanNotificationSpec extends ComponentTestSpec with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ApiSubscriptionFieldsTestData
  with CustomsNotificationService
  with FileTransmissionService
  with Eventually
  with IntegrationPatience {

  private val endpoint = s"/uploaded-batch-file-upscan-notifications/clientSubscriptionId/$subscriptionFieldsIdString"

  val repo = app.injector.instanceOf[BatchFileUploadMetadataMongoRepo]

  override protected def beforeAll() {
    await(repo.drop)
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
    await(repo.drop)
  }

  private val hasConversationId = new HasConversationId {
    override val conversationId: ConversationId = ConversationId(FileReferenceOne.value)
  }

  feature("File Transmission Notification") {
    scenario("Success request has been made to the Declaration API") {
      startFileTransmissionService()
      await(repo.create(BatchFileMetadataWithFileOneWithNoCallbackFieldsAndThree)(hasConversationId))

      Given("the Upscan Initiate service has been sent a valid request with this Declaration API as the callback URL")

      When("Upscan Notify service notifies this Declaration API using previously provided callback URL")
      val validRequest = FakeRequest("POST", endpoint).withJsonBody(readyJson())
      val result = route(app = app, validRequest).value

      Then("Declaration Service returns 204")
      status(result) shouldBe 204

      And("the response body is empty")
      contentAsString(result) shouldBe empty

      And("a request is made to File Transmission Service")
      val (requestHeaders, requestPayload) = eventually(aRequestWasMadeToFileTransmissionService())

      And("The Content-Type header is application/json")
      requestHeaders.get(CONTENT_TYPE) shouldBe Some("application/json")

      And("The Accept header is application/json")
      requestHeaders.get(ACCEPT) shouldBe Some("application/json")

      And("The User Agent header is application/json")
      requestHeaders.get(USER_AGENT) shouldBe Some("customs-declarations")

      And("The request XML payload contains details of the success outcome")
      Json.parse(requestPayload) shouldBe Json.parse(expectedFileTransmissionRequest)
    }

    scenario("Failure request has been made to the Declaration API") {
      notificationServiceIsRunning()
      await(repo.create(BatchFileMetadataWithFileOneWithNoCallbackFieldsAndThree)(hasConversationId))

      Given("the Upscan Initiate service has been sent a valid request with this Declaration API as the callback URL")

      When("Upscan Notify service notifies this Declaration API using previously provided callback URL")
      val validRequest = FakeRequest("POST", endpoint).withJsonBody(FailedJson)
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

      And("The request XML payload contains details of the failure outcome")
      trim(string2xml(requestPayload)) shouldBe trim(UpscanNotificationFailedCustomsNotificationXml)
    }

    scenario("Success request has been made to the Declaration API but metadata record does not exist in the database") {
      notificationServiceIsRunning()
      await(repo.create(BatchFileMetadataWithFileOneWithNoCallbackFieldsAndThree)(hasConversationId))

      Given("the Upscan Initiate service has been sent a valid request with this Declaration API as the callback URL")

      When("Upscan Notify service notifies this Declaration API using previously provided callback URL")
      val validRequest = FakeRequest("POST", endpoint).withJsonBody(readyJson(FileReferenceTwo))
      val result = route(app = app, validRequest).value

      Then("Declaration Service returns 500")
      status(result) shouldBe 500

      And("the response body is internal error Json")
      contentAsString(result) shouldBe UpscanNotificationInternalServerErrorJson
    }
  }

  private val expectedFileTransmissionRequest =
    """{
      |  "batch": {
      |    "id": "48400000-8cf0-11bd-b23e-10b96e4ef001",
      |    "fileCount": 2
      |  },
      |  "callbackUrl": "http://localhost:11111/file/transmission",
      |  "file": {
      |    "reference": "31400000-8ce0-11bd-b23e-10b96e4ef00f",
      |    "name": "test.pdf",
      |    "mimeType": "application/pdf",
      |    "checksum": "1a2b3c4d5e",
      |    "location": "https://a.b.com",
      |    "sequenceNumber": 1,
      |    "size": 1
      |  },
      |  "interface": {
      |    "name": "DEC64",
      |    "version": "1.0.0"
      |  },
      |  "properties": [
      |    {
      |      "name": "DeclarationId",
      |      "value": "3"
      |    },
      |    {
      |      "name": "Eori",
      |      "value": "123"
      |    },
      |    {
      |      "name": "ContentType",
      |      "value": "Document Type 1"
      |    }
      |  ]
      |}""".stripMargin
}
