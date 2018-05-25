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

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, OptionValues}
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionTwo}
import util.{AuditService, TestData}
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData._
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, UpscanInitiateService}

import scala.concurrent.Future

class FileUploadSpec extends ComponentTestSpec with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with UpscanInitiateService
  with ApiSubscriptionFieldsService
  with AuthService
  with AuditService{

  private val endpoint = "/file-upload"

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)

  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  private val validUpscanInitiateResponse =
    """<fileUpload>
      |<href>https://bucketName.s3.eu-west-2.amazonaws.com</href>
      |<fields>
      |<X-Amz-Algorithm>AWS4-HMAC-SHA256</X-Amz-Algorithm>
      |<X-Amz-Expiration>2018-02-09T12:35:45.297Z</X-Amz-Expiration>
      |<X-Amz-Signature>xxxx</X-Amz-Signature>
      |<key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
      |<acl>private</acl>
      |<X-Amz-Credential>ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request</X-Amz-Credential>
      |<policy>xxxxxxxx==</policy>
      |</fields>
      |</fileUpload>""".stripMargin

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("File upload API authorises submissions from Software Houses with v2.0 accept header") {

    scenario("An unauthorised CSP is not allowed to submit a file upload request") {
      Given("A CSP wants to submit a valid file upload")
      val request: FakeRequest[AnyContentAsXml] = ValidFileUploadRequest.fromCsp.postTo(endpoint)

      And("the CSP is unauthorised with its privileged application")
      authServiceUnauthorisesScopeForCSP()
      authServiceUnauthorisesCustomsEnrolmentForNonCSP(cspBearerToken)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 401 (UNAUTHORIZED) status is received")
      status(result) shouldBe UNAUTHORIZED

      And("the response body is empty")
      string2xml(contentAsString(result)) shouldBe string2xml(UnauthorisedRequestError)

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForCsp()
    }

    scenario("A non-CSP successfully submits a file upload request on behalf of somebody with Customs enrolment") {
      Given("A Software House wants to submit a valid file upload request")
      startUpscanInitiateService()
      val request: FakeRequest[AnyContentAsXml] = ValidFileUploadRequest.fromNonCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      And("declarant is enrolled with Customs having an EORI number")
      authServiceUnauthorisesScopeForCSP(nonCspBearerToken)
      authServiceAuthorizesNonCspWithEori()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body should be correct")
      string2xml(contentAsString(result)) shouldBe string2xml(validUpscanInitiateResponse)

      And("conversationId is correct")
      headers(result).get("X-Conversation-ID") shouldBe Some(upscanInitiateReference)

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForNonCsp()
    }

    scenario("A non-CSP is not authorised to submit a file upload request on behalf of somebody without Customs enrolment") {
      Given("A Software House wants to submit a valid file upload request")
      startUpscanInitiateService()
      val request: FakeRequest[AnyContentAsXml] = ValidFileUploadRequest.fromNonCsp.postTo(endpoint)

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
      val request = InvalidFileUploadRequest.fromNonCsp.postTo(endpoint)
      stubAuditService()
      authServiceUnauthorisesScopeForCSP(TestData.nonCspBearerToken)
      authServiceAuthorizesNonCspWithEori()
      startUpscanInitiateService()

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"invalid xml\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestSchemaValidationError)
    }


    scenario("Response status 400 when user submits a malformed xml payload") {
      Given("the API is available")
      val request = MalformedXmlRequest.fromNonCsp.copyFakeRequest(method = POST, uri = endpoint)
      stubAuditService()
      authServiceUnauthorisesScopeForCSP(TestData.nonCspBearerToken)
      authServiceAuthorizesNonCspWithEori()
      startUpscanInitiateService()

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
      val request = InvalidFileUploadRequest.fromNonCsp
        .withJsonBody(JsObject(Seq("something" -> JsString("I am a json"))))
        .copyFakeRequest(method = POST, uri = endpoint)
      stubAuditService()
      authServiceUnauthorisesScopeForCSP(TestData.nonCspBearerToken)
      authServiceAuthorizesNonCspWithEori()
      startUpscanInitiateService()

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


    scenario("Response status 406 when user submits a request without Accept header") {
      Given("the API is available")
      val request = NoAcceptHeaderFileUploadRequest.copyFakeRequest(method = POST, uri = endpoint)

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

    scenario("Response status 401 when user provides invalid token") {
      Given("the API is available")
      authServiceUnauthorisesScopeForCSP(bearerToken = invalidBearerToken)
      authServiceUnauthorisesCustomsEnrolmentForNonCSP(bearerToken = invalidBearerToken)
      val request = ValidFileUploadRequest.withCustomToken(invalidBearerToken).postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 401 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe UNAUTHORIZED
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is \"Unauthorised request\"")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(UnauthorisedRequestError)
    }

    scenario("Response status 403 when CSP sends the request (with badge identifier)") {
      Given("request is sent by CSP")
      authServiceAuthorizesCSP()
      authServiceUnauthorisesCustomsEnrolmentForNonCSP(bearerToken = cspBearerToken)
      val request = ValidFileUploadRequest.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 403 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe FORBIDDEN
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is \"Forbidden\"")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(ForbiddenError)
    }

    scenario("Response status 400 when CSP sends the request (without badge identifier)") {
      Given("request is sent by CSP")
      authServiceAuthorizesCSP()
      authServiceUnauthorisesCustomsEnrolmentForNonCSP(bearerToken = cspBearerToken)
      val request = ValidFileUploadRequestWithoutBadgeId.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is \"Forbidden\"")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestErrorXBadgeIdentifierMissingOrInvalid)
    }

    scenario("Response status 406 when user submits a request with an invalid Accept header") {
      Given("the API is available")
      val request = InvalidAcceptHeaderFileUploadRequest.copyFakeRequest(method = POST, uri = endpoint)

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
      val request = InvalidContentTypeHeaderFileUploadRequest.copyFakeRequest(method = POST, uri = endpoint)

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


    scenario("Response status 500 when user submits a valid request but downstream call fails with an HTTP error") {

      Given("the API is available")
      val request = ValidFileUploadRequest.fromNonCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      setupUpscanInitiateToReturn(status = NOT_FOUND)
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
