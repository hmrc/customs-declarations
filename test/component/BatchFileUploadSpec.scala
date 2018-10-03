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
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData._
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, GoogleAnalyticsService, UpscanInitiateService}
import util.{AuditService, TestData}

import scala.concurrent.Future

class BatchFileUploadSpec extends ComponentTestSpec with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with UpscanInitiateService
  with ApiSubscriptionFieldsService
  with AuthService
  with AuditService
  with GoogleAnalyticsService {

  private val endpoint = "/batch-file-upload"

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)

  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Valid request is processed correctly") {
    scenario("Response status 200 when user submits correct request") {
      Given("the API is available")
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)
      val request = ValidBatchFileUploadV2Request.fromNonCsp.postTo(endpoint)
      setupWiremockExpectations()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      headers(result).get(X_CONVERSATION_ID_NAME) shouldBe 'defined
    }
  }

  feature("Unauthorized batch file upload API submissions are processed correctly") {

    scenario("An unauthorised CSP is not allowed to submit a batch file upload request with v2.0 accept header") {
      Given("A CSP wants to submit a valid file upload")
      val request: FakeRequest[AnyContentAsXml] = ValidBatchFileUploadV2Request.fromCsp.postTo(endpoint)

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
      eventually(verifyAuthServiceCalledForCsp())
    }

    scenario("An unauthorised CSP is not allowed to submit a batch file upload request with v3.0 accept header") {
      Given("A CSP wants to submit a valid file upload")
      val request: FakeRequest[AnyContentAsXml] = ValidBatchFileUploadV3Request.fromCsp.postTo(endpoint)

      setupGoogleAnalyticsServiceToReturn(ACCEPTED)

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
      eventually(verifyAuthServiceCalledForCsp())
    }
  }

  feature("The API handles errors as expected") {
    scenario("Response status 400 when user submits a non-xml payload") {
      Given("the API is available")
      val request = InvalidFileUploadRequest.fromNonCsp
        .withJsonBody(JsObject(Seq("something" -> JsString("I am a json"))))
        .copyFakeRequest(method = POST, uri = endpoint)
      setupWiremockExpectations()

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

    scenario("Response status 400 when user submits invalid request") {
      Given("the API is available")
      val request = ValidSubmission_13_INV_Request.fromNonCsp.postTo(endpoint)
      setupWiremockExpectations()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then(s"a response with a 400 status is received")

      status(result) shouldBe BAD_REQUEST
      headers(result).get(X_CONVERSATION_ID_NAME) shouldBe 'defined
    }

    scenario("Response status 400 when user submits a malformed xml payload") {
      Given("the API is available")
      val request = MalformedXmlRequest.fromNonCsp.copyFakeRequest(method = POST, uri = endpoint)
      setupWiremockExpectations()

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
  }

  private def setupWiremockExpectations(): Unit = {
    stubAuditService()
    authServiceUnauthorisesScopeForCSP(TestData.nonCspBearerToken)
    authServiceAuthorizesNonCspWithEori()
    startUpscanInitiateService()
  }
}
