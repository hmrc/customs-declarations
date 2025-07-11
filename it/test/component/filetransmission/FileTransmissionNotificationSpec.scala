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

package component.filetransmission

import component.{ComponentTestSpec, ExpectedTestResponses}
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION, CONTENT_TYPE, contentAsString, route, status, *}
import uk.gov.hmrc.customs.declaration.model.ConversationId
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataMongoRepo
import uk.gov.hmrc.customs.declaration.xml.ValidateXmlAgainstSchema
import util.ApiSubscriptionFieldsTestData
import util.CustomsDeclarationsExternalServicesConfig.CustomsNotificationAuthHeaderValue
import util.FileTransmissionTestData.*
import util.TestData.*
import util.XmlOps.stringToXml
import util.externalservices.CustomsNotificationService

import java.io.StringReader
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
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

  val repo: FileUploadMetadataMongoRepo = app.injector.instanceOf[FileUploadMetadataMongoRepo]

  private val schemaFileUploadNotificationLocationV1: Schema = ValidateXmlAgainstSchema.getSchema(xsdFileUploadNotificationLocationV1).get

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    await(repo.collection.deleteMany(Filters.exists("_id")).toFuture())
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }


  private val hasConversationId = new HasConversationId {
    override val conversationId: ConversationId = ConversationId(FileReferenceOne.value)
  }

  Feature("File Transmission Notification") {
    Scenario("Success request has been made to Customs Notification service") {
      notificationServiceIsRunning()
      Given("the File Transmission service sends a notification")

      And("and a FileMetadata record with a FileName exists for FileReferenceOne in the database")
      await(repo.create(FileMetadataWithFileOne)(hasConversationId))

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

      And("The User-Agent header is customs-declarations")
      requestHeaders.get(USER_AGENT) shouldBe Some("customs-declarations")

      And("The request XML payload contains details of the scan outcome")
      trim(stringToXml(requestPayload)) shouldBe trim(FileTransmissionSuccessCustomsNotificationXml)
      schemaFileUploadNotificationLocationV1.newValidator().validate(new StreamSource(new StringReader(requestPayload)))
    }

    Scenario("Response status 400 when File Transmission service sends invalid payload") {
      Given("the File Transmission service sends a notification")

      When("File Transmission service notifies Declaration API using previously provided callback URL with invalid json payload")
      val invalidRequest = FakeRequest("POST", endpoint).withJsonBody(Json.parse(InvalidFileTransmissionNotificationPayload))
      val result = route(app = app, invalidRequest).value

      Then("a response with a 400 status is returned")
      status(result) should be (BAD_REQUEST)
    }
  }

}
