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
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionThree, VersionTwo}
import util.FakeRequests._
import util.RequestHeaders.{ValidHeadersV2, ValidHeadersV3}
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, GoogleAnalyticsService, MdgStatusDeclarationService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig}

import scala.concurrent.Future

class CustomsDeclarationStatusSpec extends ComponentTestSpec with AuditService with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MdgStatusDeclarationService
  with ApiSubscriptionFieldsService
  with AuthService
  with GoogleAnalyticsService {

  private val mrn = "some-mrn"
  private val endpoint = s"/status-request/mrn/$mrn"

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)

  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  private val apiSubscriptionKeyForXClientIdV3 = apiSubscriptionKeyForXClientIdV2.copy(version = VersionThree)

  private val validResponse =
    """<stat:declarationManagementInformationResponse xmlns:stat="http://gov.uk/customs/declarations/status-request">
      |  <stat:declaration>
      |    <stat:versionNumber>0</stat:versionNumber>
      |    <stat:creationDate formatCode="string">2001-12-17T09:30:47Z</stat:creationDate>
      |    <stat:goodsItemCount>2</stat:goodsItemCount>
      |    <stat:tradeMovementType>trade movement type</stat:tradeMovementType>
      |    <stat:type>declaration type</stat:type>
      |    <stat:packageCount>3</stat:packageCount>
      |    <stat:acceptanceDate>2002-12-17T09:30:47Z</stat:acceptanceDate>
      |    <stat:parties>
      |      <stat:partyIdentification>
      |        <stat:number>1</stat:number>
      |      </stat:partyIdentification>
      |    </stat:parties>
      |  </stat:declaration>
      |</stat:declarationManagementInformationResponse>""".stripMargin

  val validRequestV2 = FakeRequest("GET", endpoint).withHeaders(ValidHeadersV2.toSeq: _*).fromCsp
  val validRequestV3 = validRequestV2.withHeaders(ValidHeadersV3.toSeq: _*)

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
    setupGoogleAnalyticsServiceToReturn(ACCEPTED)
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Declaration API authorises status requests from CSPs with v2.0 accept header") {
    scenario("An authorised CSP successfully requests a status") {
      Given("A CSP wants the status of a declaration")
      startMdgStatusV2Service()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSPNoNrs()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, validRequestV2).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCspNoNrs())

      And("v2 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContextV2))))

      And("GA call was made")
      eventually(verifyGoogleAnalyticsServiceWasCalled())
    }
  }

  feature("Declaration API authorises status requests from CSPs with v3.0 accept header") {
    scenario("An authorised CSP successfully requests a status") {
      Given("A CSP wants the status of a declaration")
      startMdgStatusV3Service()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV3)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSPNoNrs()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, validRequestV3).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      And("the response body is a valid status xml")
      contentAsString(result) shouldBe validResponse

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCspNoNrs())

      And("v3 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContextV3))))

      And("GA call was made")
      eventually(verifyGoogleAnalyticsServiceWasCalled())
    }
  }

}
