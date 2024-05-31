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

package component

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionThree, VersionTwo}
import uk.gov.hmrc.customs.declaration.xml.ValidateXmlAgainstSchema
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.TestData.conversationIdValue
import util.XmlOps._
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, MdgWcoDecService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig, TestXMLData}

import java.io.StringReader
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import scala.concurrent.Future

class CustomsDeclarationAmendSpec extends ComponentTestSpec with AuditService with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MdgWcoDecService
  with ApiSubscriptionFieldsService
  with AuthService {

  private val endpoint = "/amend"

  private val apiSubscriptionKeyForXClientIdV2 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionTwo)

  private val apiSubscriptionKeyForXClientIdV3 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionThree)

  private val schemaErrorV1: Schema = ValidateXmlAgainstSchema.getSchema(xsdErrorLocationV1).get

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
      {TestXMLData.ValidSubmissionXML}
    </v1:requestDetail>
  </v1:submitDeclarationRequest>.toString()

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  Feature("Declaration API authorises amendment of submissions from CSPs with v2.0 accept header") {
    Scenario("An authorised CSP successfully submits a customs amend declaration") {
      Given("A CSP wants to submit a valid customs amend declaration")
      startMdgWcoDecServiceV2()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      val request: FakeRequest[AnyContentAsXml] = ValidAmendV2Request.fromCsp.postTo(endpoint)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is empty")
      contentAsString(result) should be (empty)

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v2 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgWcoDecV2ServiceContext))))

      And("the payload is correct")
      verifyMdgWcoDecServiceWasCalledWithV2(expectedXml)
    }

  }

  Feature("Declaration API authorises amendment of submissions from CSPs with v3.0 accept header") {
    Scenario("An authorised CSP successfully submits a customs amend declaration") {
      Given("A CSP wants to submit a valid customs amend declaration")
      startMdgWcoDecServiceV3()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV3)

      val request: FakeRequest[AnyContentAsXml] = ValidAmendV3Request.fromCsp.postTo(endpoint)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is empty")
      contentAsString(result) should be (empty)

      And("the request was authorised with AuthService")
      verifyAuthServiceCalledForCsp()

      And("v3 config was used")
      verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgWcoDecV3ServiceContext)))
    }
  }

  Feature("Declaration API handles amendment of submission errors from CSPs as expected") {

    Scenario("Response status 400 when user submits an xml payload that does not adhere to schema having multiple errors") {
      Given("the API is available")
      val request = InvalidSubmissionRequestWith2Errors.fromCsp.postTo(endpoint)
      stubAuditService()
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then(s"a response with a 400 status is received")
      contentAsString(result) should include("BAD_REQUEST")

      status(result) shouldBe BAD_REQUEST
      headers(result).get(X_CONVERSATION_ID_NAME).value should include(conversationIdValue)

      And("the response body is a \"invalid xml\" XML")
      stringToXml(contentAsString(result)) shouldBe stringToXml(BadRequestErrorWith2Errors)
      schemaErrorV1.newValidator().validate(new StreamSource(new StringReader(BadRequestErrorWith2Errors)))
    }

  }

}
