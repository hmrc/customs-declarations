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
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc._
import play.api.test.Helpers._
import util.AuditService
import util.TestData._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.externalservices.{AuthService, MdgWcoDecService}

import scala.concurrent.Future

class CustomsDeclarationsUnhappyPathSpec extends AcceptanceTestSpec
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

  private val BadRequestErrorWith2Errors =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Payload is not valid according to schema</message>
      |  <errors>
      |     <error>
      |       <code>xml_validation_error</code>
      |       <message>cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'.</message>
      |     </error>
      |     <error>
      |       <code>xml_validation_error</code>
      |       <message>cvc-datatype-valid.1.2.1: 'ABC' is not a valid value for 'decimal'.</message>
      |     </error>
      |  </errors>
      |</errorResponse>
    """.stripMargin

  private val MalformedXmlBodyError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Request body does not contain a well-formed XML document.</message>
      |</errorResponse>
    """.stripMargin

  private val BadRequestErrorXBatchIdentifierMissingorInvalid =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>X-Badge-Identifier header is missing or invalid</message>
      |</errorResponse>
    """.stripMargin

  private val InvalidAcceptHeaderError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>ACCEPT_HEADER_INVALID</code>
      |  <message>The accept header is missing or invalid</message>
      |</errorResponse>
    """.stripMargin

  private val InvalidContentTypeHeaderError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>UNSUPPORTED_MEDIA_TYPE</code>
      |  <message>The content type header is missing or invalid</message>
      |</errorResponse>
    """.stripMargin

  private val InternalServerError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>INTERNAL_SERVER_ERROR</code>
      |  <message>Internal server error</message>
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

    scenario("Response status 400 when user submits an xml payload that does not adhere to schema") {
      Given("the API is available")
      val request = InvalidRequest.fromCsp.postTo(endpoint)

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

    scenario("Response status 400 when user submits an xml payload that does not adhere to schema having multiple errors") {
      Given("the API is available")
      val request = InvalidRequestWith3Errors.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"invalid xml\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestErrorWith2Errors)
    }

    scenario("Response status 400 when user submits a malformed xml payload") {
      Given("the API is available")
      val request = MalformedXmlRequest.copyFakeRequest(method = POST, uri = endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"malformed xml body\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(MalformedXmlBodyError)
    }

    scenario("Response status 400 when user submits a non-xml payload") {
      Given("the API is available")
      val request = ValidRequest
        .withJsonBody(JsObject(Seq("something" -> JsString("I am a json"))))
        .copyFakeRequest(method = POST, uri = endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"malformed xml body\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(MalformedXmlBodyError)
    }

    scenario("Response status 500 when user submits a request without any client id headers") {
      Given("the API is available")
      val request = NoClientIdIdHeaderRequest.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then("a response with a 500 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe INTERNAL_SERVER_ERROR
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined
    }

    scenario("Response status 406 when user submits a request without Accept header") {
      Given("the API is available")
      val request = NoAcceptHeaderRequest.copyFakeRequest(method = POST, uri = endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 406 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe NOT_ACCEPTABLE
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"invalid Accept header\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(InvalidAcceptHeaderError)
    }

    scenario("Response status 406 when user submits a request with an invalid Accept header") {
      Given("the API is available")
      val request = InvalidAcceptHeaderRequest.copyFakeRequest(method = POST, uri = endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 406 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe NOT_ACCEPTABLE
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is an \"invalid Accept header\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(InvalidAcceptHeaderError)
    }

    scenario("Response status 415 when user submits a request with an invalid Content-Type header") {
      Given("the API is available")
      val request = InvalidContentTypeHeaderRequest.copyFakeRequest(method = POST, uri = endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then("a response with a 415 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe UNSUPPORTED_MEDIA_TYPE
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is an \"invalid Content-Type header\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(InvalidContentTypeHeaderError)
    }

    scenario("Response status 400 when a CSP user submits a request without a X-Badge-Identifier header") {
      Given("the API is available")
      val request = InvalidRequestWithoutXBadgeIdentifier.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then("a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"Bad Request\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestErrorXBatchIdentifierMissingorInvalid)
    }

    scenario("Response status 400 when a CSP user submits a request with an invalid X-Badge-Identifier header") {
      Given("the API is available")
      val request = InvalidRequestWithInvalidXBadgeIdentifier.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then("a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"Bad Request\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestErrorXBatchIdentifierMissingorInvalid)
    }

    scenario("Response status 500 when user submits a valid request but downstream call to DMS fails with an HTTP error") {

      Given("the API is available")
      val request = ValidRequest.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      setupMdgWcoDecServiceToReturn(status = NOT_FOUND)
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 500 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe INTERNAL_SERVER_ERROR
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is an \"Internal server error\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(InternalServerError)
    }
  }

}
