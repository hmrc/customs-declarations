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

import org.scalatest.{Matchers, OptionValues}
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionTwo}
import util.AuditService
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, MdgWcoDecService}

import scala.concurrent.Future

//TODO: Confirm with QA's that schema tests like these that give confidence around schema changes are tested manually by QA's
class DeclarationSchemaSpec extends ComponentTestSpec
  with Matchers with OptionValues with AuthService with MdgWcoDecService with ApiSubscriptionFieldsService with AuditService {

  private val endpoint = "/"


  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)
  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)


  override protected def beforeAll() {
    startMockServer()
    stubAuditService()
    authServiceAuthorizesCSP()
    startMdgWcoDecService()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("The API handles cancellation-specific type code (INV) correctly") {

    scenario("Response status 400 when user submits function code 13 + type code INV") {
      Given("the API is available")
      val request = ValidSubmission_13_INV_Request.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined
    }


    scenario("Response status 202 when authorised as CSP with privileged application and submits function code 13 in the request") {
      Given("the API is available")
      val request = ValidSubmission_13_Request.fromCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)
      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED
    }

    scenario("Response status 400 when authorised as CSP with privileged application and submits type code INV in the request") {
      Given("the API is available")
      val request = ValidSubmission_13_INV_Request.fromCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)
      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val resultFuture: Future[Result] = route(app = app, request).value

      Then("a response with a 400 (BADREQUEST) status is received")
      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined
    }

    scenario("Response status 202 when function code 13 is in the request") {
      Given("the API is available")
      val request = ValidSubmission_13_Request.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED
    }


    scenario("Response status 400 when user submits type code INV in the request") {
      Given("the API is available")
      val request = ValidSubmission_INV_Request.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined
    }
  }

}
