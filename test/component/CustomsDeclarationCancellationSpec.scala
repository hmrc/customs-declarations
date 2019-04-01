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
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionThree, VersionTwo}
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.XmlOps.stringToXml
import util.externalservices._
import util.{AuditService, CustomsDeclarationsExternalServicesConfig, TestXMLData}

import scala.concurrent.Future

class CustomsDeclarationCancellationSpec extends ComponentTestSpec with AuditService with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MdgCancellationDeclarationService
  with ApiSubscriptionFieldsService
  with AuthService {

  private val endpoint = "/cancellation-requests"

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", VersionOne)

  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  private val apiSubscriptionKeyForXClientIdV3 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionThree)

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  val expectedXml = <v1:submitDeclarationRequest xmlns:v1="http://uk/gov/hmrc/mdg/declarationmanagement/submitdeclaration/request/schema/v1" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns:n1="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:p1="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <v1:requestCommon>
      <!--type: regimeType-->
      <v1:regime>CDS</v1:regime>
      <v1:receiptDate>2019-01-01T12:00:00Z</v1:receiptDate>
      <v1:clientID>327d9145-4965-4d28-a2c5-39dedee50334</v1:clientID>
      <v1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</v1:conversationID>
      <v1:badgeIdentifier>BADGEID123</v1:badgeIdentifier>
      <v1:originatingPartyID>ZZ123456789000</v1:originatingPartyID>
      <v1:authenticatedPartyID>ZZ123456789000</v1:authenticatedPartyID>
    </v1:requestCommon>
    <v1:requestDetail>
      {TestXMLData.validCancellationXML()}
    </v1:requestDetail>
  </v1:submitDeclarationRequest>.toString()

  feature("Declaration API authorises cancellation of submissions from CSPs with v1.0 accept header") {
    scenario("An authorised CSP successfully submits a cancellation request") {
      Given("A CSP wants to submit a valid cancellation request")
      startMdgCancellationV1Service()
      val request: FakeRequest[AnyContentAsXml] = ValidCancellationRequestWithV1AcceptHeader.fromCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV1)

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

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContext))))

      And("the payload is correct")
      verifyMdgWcoDecServiceWasCalledWithV1(expectedXml)
    }
  }

  feature("Declaration API authorises cancellation of submissions from CSPs with v2.0 accept header") {
    scenario("An authorised CSP successfully submits a cancellation request") {
      Given("A CSP wants to submit a valid cancellation request")
      startMdgCancellationV2Service()
      val request: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request.fromCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

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
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContextV2))))

      And("the payload is correct")
      verifyMdgWcoDecServiceWasCalledWithV2(expectedXml)
    }
  }
    feature("Declaration API authorises cancellation of submissions from CSPs with v3.0 accept header") {
      scenario("An authorised CSP successfully submits a cancellation request") {
        Given("A CSP wants to submit a valid cancellation request")
        startMdgCancellationV3Service()
        val request: FakeRequest[AnyContentAsXml] = ValidCancellationV3Request.fromCsp.postTo(endpoint)
        startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV3)

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
        eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContextV3))))
      }
    }

  feature("Declaration API handles cancellation of submission errors from CSPs as expected") {

    scenario("Response status 400 when user submits an xml payload that does not adhere to schema having multiple errors") {
      Given("the API is available")
      stubAuditService()
      authServiceAuthorizesCSP()
      val request = InvalidCancellationRequestWith2Errors.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"invalid xml\" XML")
      stringToXml(contentAsString(resultFuture)) shouldBe stringToXml(BadRequestErrorWith2Errors)
    }

  }
}
