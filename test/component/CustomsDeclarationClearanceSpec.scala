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

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, OptionValues}
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionTwo}
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData._
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, MdgWcoDecService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig, RequestHeaders}

import scala.concurrent.Future

class CustomsDeclarationClearanceSpec extends ComponentTestSpec with AuditService with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MdgWcoDecService
  with ApiSubscriptionFieldsService
  with AuthService {

  private val endpoint = "/clearance"

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)

  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  protected override val BadRequestErrorWith2Errors =
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

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
    startMdgWcoDecService()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Declaration API authorises submissions from CSPs and Software Houses with v2.0 accept header") {
    scenario("An authorised CSP successfully submits a customs clearance declaration") {
      Given("A CSP wants to submit a valid customs clearance declaration")

      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      val request: FakeRequest[AnyContentAsXml] = ValidClearanceRequest.fromCsp.postTo(endpoint)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is empty")
      contentAsString(result) shouldBe 'empty

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForCsp()

      And("v2 config was used")
      verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgWcoDecV2ServiceContext)))
    }

    scenario("An unauthorised CSP is not allowed to submit a customs clearance declaration") {
      Given("A CSP wants to submit a valid customs clearance declaration")
      val request: FakeRequest[AnyContentAsXml] = ValidClearanceRequest.fromCsp.postTo(endpoint)

      And("the CSP is unauthorised with its privileged application")
      authServiceUnauthorisesScopeForCSP()
      authServiceUnauthorisesCustomsEnrolmentForNonCSP(cspBearerToken)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 401 (UNAUTHORIZED) status is received")
      status(result) shouldBe UNAUTHORIZED

      And("the response body is Unauthorised error")
      string2xml(contentAsString(result)) shouldBe string2xml(UnauthorisedRequestError)

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForCsp()
    }

    scenario("A non-CSP successfully submits a declaration on behalf of somebody with Customs enrolment") {
      Given("A Software House wants to submit a valid customs clearance declaration")

      val request: FakeRequest[AnyContentAsXml] = ValidClearanceRequest.fromNonCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      And("declarant is enrolled with Customs having an EORI number")
      authServiceUnauthorisesScopeForCSP(nonCspBearerToken)
      authServiceAuthorizesNonCspWithEori()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is empty")
      contentAsString(result) shouldBe 'empty

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForNonCsp()
    }

    scenario("A non-CSP is not authorised to submit a declaration on behalf of somebody without Customs enrolment") {
      Given("A Software House wants to submit a valid customs clearance declaration")

      val request: FakeRequest[AnyContentAsXml] = ValidClearanceRequest.fromNonCsp.postTo(endpoint)

      And("declarant is not enrolled with Customs")
      authServiceUnauthorisesScopeForCSP(nonCspBearerToken)
      authServiceUnauthorisesCustomsEnrolmentForNonCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 401 (UNAUTHORIZED) status is received")
      status(result) shouldBe UNAUTHORIZED

      And("the response body is empty")
      string2xml(contentAsString(result)) shouldBe string2xml(UnauthorisedRequestError)

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForNonCsp()
    }
  }

  //*** UNHAPPY PATH SPECS ***

  feature("The API handles errors as expected") {

    scenario("Response status 400 when user submits an xml payload that does not adhere to schema") {
      Given("the API is available")
      val request = InvalidSubmissionRequest.fromCsp.postTo(endpoint)
      stubAuditService()
      authServiceAuthorizesCSP()

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
      val request = InvalidSubmissionRequestWith3Errors.fromCsp.postTo(endpoint)
      stubAuditService()
      authServiceAuthorizesCSP()

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
      val request = MalformedXmlRequest.fromCsp.copyFakeRequest(method = POST, uri = endpoint)
      stubAuditService()
      authServiceAuthorizesCSP()

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
      val request = ValidSubmissionRequest.fromCsp
        .withJsonBody(JsObject(Seq("something" -> JsString("I am a json"))))
        .copyFakeRequest(method = POST, uri = endpoint)
      stubAuditService()
      authServiceAuthorizesCSP()

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
      val request = NoClientIdIdHeaderSubmissionRequest.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then("a response with a 500 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe INTERNAL_SERVER_ERROR
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is an \"Internal server error\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(InternalServerError)
    }

    scenario("Response status 406 when user submits a request without Accept header") {
      Given("the API is available")
      val request = NoAcceptHeaderSubmissionRequest.copyFakeRequest(method = POST, uri = endpoint)

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
      val request = InvalidAcceptHeaderSubmissionRequest.copyFakeRequest(method = POST, uri = endpoint)

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
      val request = InvalidContentTypeHeaderSubmissionRequest.copyFakeRequest(method = POST, uri = endpoint)
      stubAuditService()
      authServiceAuthorizesCSP()

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
      val request = InvalidSubmissionRequestWithoutXBadgeIdentifier.fromCsp.postTo(endpoint)
      stubAuditService()
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then("a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"Bad Request\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestErrorXBadgeIdentifierMissingOrInvalid)
    }

    scenario("Response status 400 when a CSP user submits a request with an invalid X-Badge-Identifier header") {
      Given("the API is available")
      val request = InvalidSubmissionRequestWithInvalidXBadgeIdentifier.fromCsp.postTo(endpoint)
      stubAuditService()
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then("a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"Bad Request\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestErrorXBadgeIdentifierMissingOrInvalid)
    }

    scenario("Response status 500 when user submits a valid request but downstream call to DMS fails with an HTTP error") {

      Given("the API is available")
      val request = ValidSubmissionRequest.fromCsp.postTo(endpoint)

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
