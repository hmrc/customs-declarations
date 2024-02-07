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

package component

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionTwo}
import util.AuditService
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData.conversationIdValue
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, MdgWcoDecService}

import scala.concurrent.Future

class DeclarationSchemaSpec extends ComponentTestSpec
  with Matchers with OptionValues with AuthService with MdgWcoDecService with ApiSubscriptionFieldsService with AuditService {

  private val endpoint = "/"


  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)
  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)


  override protected def beforeAll(): Unit = {
    startMockServer()
    stubAuditService()
    authServiceAuthorizesCSP()
    startMdgWcoDecServiceV2()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  Feature("The API handles cancellation-specific type code (INV) correctly") {

    Scenario("Response status 202 when user submits function code 13 + type code INV") {
      Given("the API is available")
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)
      val request = ValidSubmission_13_INV_Request.fromCsp.postTo(endpoint)
      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then(s"a response with a 202 status is received")
      contentAsString(result) should be (empty)

      status(result) shouldBe ACCEPTED
      headers(result).get(X_CONVERSATION_ID_NAME).value should include(conversationIdValue)
    }

    Scenario("Response status 202 when authorised as CSP with privileged application and submits function code 13 in the request") {
      Given("the API is available")
      val request = ValidSubmission_13_Request.fromCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)
      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED
      contentAsString(result) should be (empty)
    }

    Scenario("Response status 202 when authorised as CSP with privileged application and submits type code INV in the request") {
      Given("the API is available")
      val request = ValidSubmission_13_INV_Request.fromCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)
      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val resultFuture: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(resultFuture) shouldBe ACCEPTED
      headers(resultFuture).get(X_CONVERSATION_ID_NAME).value should include(conversationIdValue)
    }

    Scenario("Response status 202 when function code 13 is in the request") {
      Given("the API is available")
      val request = ValidSubmission_13_Request.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED
      contentAsString(result) should be (empty)
    }


    Scenario("Response status 202 when user submits type code INV in the request") {
      Given("the API is available")
      val request = ValidSubmission_INV_Request.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then(s"a response with a 202 status is received")
      contentAsString(result) should be (empty)


      status(result) shouldBe ACCEPTED
      headers(result).get(X_CONVERSATION_ID_NAME).value should include(conversationIdValue)
    }
  }

}
