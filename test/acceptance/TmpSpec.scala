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

package acceptance

import org.scalatest.{Matchers, OptionValues}
import play.api.mvc._
import play.api.test.Helpers._
import util.AuditService
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.externalservices.{AuthService, MdgWcoDecService}

import scala.concurrent.Future

class TmpSpec extends AcceptanceTestSpec
  with Matchers with OptionValues with AuthService with MdgWcoDecService with AuditService {

  private val endpoint = "/"

  private val BadRequestError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Payload is not valid according to schema</message>
      |  <errors>
      |     <error>
      |       <code>xml_validation_error</code>
      |       <message>cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'.</message>
      |     </error>
      |  </errors>
      |</errorResponse>
    """.stripMargin


  override protected def beforeAll() {
    startMockServer()
    stubAuditService()
    authServiceAuthorizesCSP()
    startMdgWcoDecService()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("The API handles errors as expected") {

    scenario("Response status 400 when user submits 13 + INV request") {
      Given("the API is available")
      val request = ValidSubmission_13_INV_Request.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"invalid xml\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestError)
    }


    scenario("Response status 202 when user submits 13  request") {
      Given("the API is available")
      val request = ValidSubmission_13_Request.fromCsp.postTo(endpoint)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED
    }


    scenario("Response status 202 when user submits INV request") {
      Given("the API is available")
      val request = ValidSubmission_INV_Request.fromCsp.postTo(endpoint)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED
    }
  }

}
