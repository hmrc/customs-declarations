/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionThree, VersionTwo}
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, MdgWcoDecService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig}

import scala.concurrent.Future

class CustomsDeclarationArrivalNotificationSpec extends ComponentTestSpec with AuditService with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MdgWcoDecService
  with ApiSubscriptionFieldsService
  with AuthService {

  private val endpoint = "/arrival-notification"

  private val apiSubscriptionKeyForXClientIdV2 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionTwo)

  private val apiSubscriptionKeyForXClientIdV3 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionThree)

  protected override val BadRequestErrorWith2Errors: String =
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
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Declaration API authorises arrival notification submissions from CSPs with v2.0 accept header") {
    scenario("An authorised CSP successfully submits a customs arrival notification declaration") {
      Given("A CSP wants to submit a valid customs arrival notification declaration")
      startMdgWcoDecServiceV2()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      val request: FakeRequest[AnyContentAsXml] = ValidArrivalNotificationV2Request.fromCsp.postTo(endpoint)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is empty")
      contentAsString(result) shouldBe 'empty

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v2 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgWcoDecV2ServiceContext))))
    }

  }

  feature("Declaration API authorises arrival notification submissions from CSPs with v3.0 accept header") {
    scenario("An authorised CSP successfully submits a customs arrival notification declaration") {
      Given("A CSP wants to submit a valid customs arrival notification declaration")
      startMdgWcoDecServiceV3()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV3)

      val request: FakeRequest[AnyContentAsXml] = ValidArrivalNotificationV3Request.fromCsp.postTo(endpoint)

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

      And("v3 config was used")
      verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgWcoDecV3ServiceContext)))
    }

  }

  feature("Declaration API handles arrival-notification submission errors from CSPs as expected") {

    scenario("Response status 400 when user submits an xml payload that does not adhere to schema having multiple errors") {
      Given("the API is available")
      val request = InvalidSubmissionRequestWith2Errors.fromCsp.postTo(endpoint)
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

  }

}
