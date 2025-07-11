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

package component.upscan

import component.{ComponentTestSpec, ExpectedTestResponses}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import play.api.libs.json.{JsObject, JsString}
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, *}
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionTwo}
import uk.gov.hmrc.customs.declaration.xml.ValidateXmlAgainstSchema
import util.FakeRequests.*
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData.*
import util.XmlOps.stringToXml
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, UpscanInitiateService}
import util.{AuditService, TestData}

import java.io.StringReader
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import scala.concurrent.Future

class FileUploadSpec extends ComponentTestSpec with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with UpscanInitiateService
  with ApiSubscriptionFieldsService
  with AuthService
  with AuditService {

  private val endpoint = "/file-upload"
  private val schemaFileUploadResponseLocationV1: Schema = ValidateXmlAgainstSchema.getSchema(xsdFileUploadResponseLocationV1).get

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)

  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  Feature("Valid request is processed correctly") {
    Scenario("Response status 200 when user submits correct request") {
      Given("the API is available")
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)
      val request = ValidFileUploadV2Request.fromNonCsp.postTo(endpoint)
      setupExternalServiceExpectations()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      headers(result).get(X_CONVERSATION_ID_NAME).value should include(conversationIdValue)
      schemaFileUploadResponseLocationV1.newValidator().validate(new StreamSource(new StringReader(contentAsString(result))))
    }
  }

  Feature("Unauthorized file upload API submissions are processed correctly") {

    Scenario("An unauthorised CSP is not allowed to submit a file upload request with v2.0 accept header") {
      Given("A CSP wants to submit a valid file upload")
      val request: FakeRequest[AnyContentAsXml] = ValidFileUploadV2Request.fromCsp.postTo(endpoint)

      And("the CSP is unauthorised with its privileged application")
      authServiceUnauthorisesScopeForCSPWithoutRetrievals()
      authServiceUnauthorisesCustomsEnrolmentForNonCSPWithoutRetrievals(cspBearerToken)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 401 (UNAUTHORIZED) status is received")
      status(result) shouldBe UNAUTHORIZED

      And("the response body is empty")
      stringToXml(contentAsString(result)) shouldBe stringToXml(UnauthorisedRequestError)

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCspWithoutRetrievals())
    }

    Scenario("An unauthorised CSP is not allowed to submit a file upload request with v3.0 accept header") {
      Given("A CSP wants to submit a valid file upload")
      val request: FakeRequest[AnyContentAsXml] = ValidFileUploadV3Request.fromCsp.postTo(endpoint)

      And("the CSP is unauthorised with its privileged application")
      authServiceUnauthorisesScopeForCSPWithoutRetrievals()
      authServiceUnauthorisesCustomsEnrolmentForNonCSPWithoutRetrievals(cspBearerToken)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 401 (UNAUTHORIZED) status is received")
      status(result) shouldBe UNAUTHORIZED

      And("the response body is empty")
      contentAsString(result) should include("UNAUTHORIZED")

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCspWithoutRetrievals())
    }
  }

  Feature("The API handles errors as expected") {
    Scenario("Response status 400 when user submits a non-xml payload") {
      Given("the API is available")
      val request = InvalidFileUploadRequest.fromNonCsp
        .withJsonBody(JsObject(Seq("something" -> JsString("I am a json"))))
        .withMethod(POST).withTarget(InvalidFileUploadRequest.target.withPath(endpoint))
      setupExternalServiceExpectations()

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      val resultFuture = result.value
      contentAsString(resultFuture) should include("BAD_REQUEST")

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME).value should include(conversationIdValue)

      And("the response body is a \"malformed xml body\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(MalformedXmlBodyError)
    }

    Scenario("Response status 400 when user submits invalid request") {
      Given("the API is available")
      val request = ValidSubmission_13_INV_Request.fromNonCsp.postTo(endpoint)
      setupExternalServiceExpectations()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then(s"a response with a 400 status is received")

      status(result) shouldBe BAD_REQUEST
      headers(result).get(X_CONVERSATION_ID_NAME).value should include(conversationIdValue)
    }

    Scenario("Response status 400 when user submits a malformed xml payload") {
      Given("the API is available")
      val request = MalformedXmlRequest.fromNonCsp
        .withMethod(POST).withTarget(InvalidFileUploadRequest.target.withPath(endpoint))
      setupExternalServiceExpectations()

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      val resultFuture = result.value

      contentAsString(resultFuture) should include("BAD_REQUEST")
      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME).value should include(conversationIdValue)

      And("the response body is a \"malformed xml body\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(MalformedXmlBodyError)
    }
  }

  private def setupExternalServiceExpectations(): Unit = {
    stubAuditService()
    authServiceUnauthorisesScopeForCSPWithoutRetrievals(TestData.nonCspBearerToken)
    authServiceAuthorizesNonCspWithEoriAndNoRetrievals()
    startUpscanInitiateService()
  }
}
