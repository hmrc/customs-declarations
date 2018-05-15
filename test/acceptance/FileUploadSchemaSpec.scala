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
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionTwo}
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, UpscanInitiateService}
import util.{AuditService, TestData}

import scala.concurrent.Future

class FileUploadSchemaSpec extends AcceptanceTestSpec
  with Matchers with OptionValues with AuthService with UpscanInitiateService with ApiSubscriptionFieldsService with AuditService {

  private val endpoint = "/file-upload"

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", version = VersionOne)
  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  override protected def beforeAll() {
    startMockServer()
    stubAuditService()
    authServiceUnauthorisesScopeForCSP(TestData.nonCspBearerToken)
    authServiceAuthorizesNonCspWithEori()
    startUpscanInitiateService()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("The API handles errors as expected") {

    scenario("Response status 400 when user submits malformed request") {
      Given("the API is available")
      val request = ValidSubmission_13_INV_Request.fromNonCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then(s"a response with a 400 status is received")

      status(result) shouldBe BAD_REQUEST
      headers(result).get(X_CONVERSATION_ID_NAME) shouldBe 'defined
    }


    scenario("Response status 200 when user submits correct request") {
      Given("the API is available")
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)
      val request = ValidFileUploadRequest.fromNonCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 200 (OK) status is received")
      status(result) shouldBe OK

      headers(result).get(X_CONVERSATION_ID_NAME) shouldBe 'defined
    }


  }

}
