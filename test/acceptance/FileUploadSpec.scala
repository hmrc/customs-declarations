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

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, OptionValues}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionTwo}
import util.FakeRequests._
import util.TestData._
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, UpscanInitiateService}

import scala.concurrent.Future

class FileUploadSpec extends AcceptanceTestSpec
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with UpscanInitiateService
  with ApiSubscriptionFieldsService
  with AuthService {

  private val endpoint = "/file-upload"

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)

  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  private val UnauthorisedError =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>UNAUTHORIZED</code>
      |  <message>Unauthorised request</message>
      |</errorResponse>
    """.stripMargin

  private val validUpscanInitiateResponse =
    """<fileUpload>
      |<href>https://bucketName.s3.eu-west-2.amazonaws.com</href>
      |<X-Amz-Algorithm>AWS4-HMAC-SHA256</X-Amz-Algorithm>
      |<X-Amz-Expiration>2018-02-09T12:35:45.297Z</X-Amz-Expiration>
      |<X-Amz-Signature>xxxx</X-Amz-Signature>
      |<key>xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</key>
      |<acl>private</acl>
      |<X-Amz-Credential>ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request</X-Amz-Credential>
      |<policy>xxxxxxxx==</policy>
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
      string2xml(contentAsString(result)) shouldBe string2xml(UnauthorisedError)

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForCsp()
    }


    scenario("A non-CSP successfully submits a declaration on behalf of somebody with Customs enrolment") {
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
      string2xml(contentAsString(result)) shouldBe string2xml(UnauthorisedError)

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForNonCsp()
    }
  }
}
