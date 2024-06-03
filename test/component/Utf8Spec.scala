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

import org.apache.pekko.util.ByteString
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import play.api.mvc._
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionTwo}
import util.AuditService
import util.FakeRequests._
import util.RequestHeaders.ValidHeadersV2WithCharset
import util.externalservices._

import java.nio.file.{Files, Paths}
import scala.concurrent.Future

class Utf8Spec extends ComponentTestSpec with AuditService with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MdgWcoDecService
  with ApiSubscriptionFieldsService
  with AuthService
  with CustomsDeclarationsMetricsService
  with NrsService {

  private val endpoint = "/"

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

  val ValidRawXmlByte = ByteString.fromArray(Files.readAllBytes(Paths.get("target/scala-2.13/test-classes/raw/valid_xml.raw")))
  val InvalidRawXmlByte = ByteString.fromArray(Files.readAllBytes(Paths.get("target/scala-2.13/test-classes/raw/invalid_xml.raw")))

  Feature("Declaration API rejects declaration payloads containing invalid utf-8 bytes") {
    Scenario(
      "Response status 200 when user submits a valid utf-8 encoded payload with Header 'Content Type: application/xml'"
    ) {
      Given("the API is available")

      startMdgWcoDecServiceV2()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      val request = ValidSubmissionRawRequest.fromCsp
        .withMethod(POST)
        .withTarget(ValidSubmissionRawRequest.target.withPath(endpoint))
        .withRawBody(ValidRawXmlByte)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 400 (BAD_REQUEST) status is received")
      status(result) shouldBe ACCEPTED
    }

    Scenario(
      "Response status 400 when user submits a payload containing a non-utf-8 byte with Header 'Content Type: application/xml'"
    ) {
      Given("the API is available")

      startMdgWcoDecServiceV2()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      val request = ValidSubmissionRawRequest.fromCsp
        .withMethod(POST)
        .withTarget(ValidSubmissionRawRequest.target.withPath(endpoint))
        .withRawBody(InvalidRawXmlByte)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 400 (BAD_REQUEST) status is received")
      status(result) shouldBe BAD_REQUEST
    }

    Scenario(
      "Response status 200 when user submits a valid utf-8 encoded payload with Header 'Content Type: application/xml; charset=UTF-8'"
    ) {
      Given("the API is available")

      startMdgWcoDecServiceV2()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      val request = ValidSubmissionRawRequest.fromCsp
        .withMethod(POST)
        .withHeaders(ValidHeadersV2WithCharset.toSeq: _*)
        .withTarget(ValidSubmissionRawRequest.target.withPath(endpoint))
        .withRawBody(ValidRawXmlByte)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED
    }

    Scenario(
      "Response status 400 when user submits a payload containing a non-utf-8 byte with Header 'Content Type: application/xml; charset=UTF-8'"
    ) {
      Given("the API is available")

      startMdgWcoDecServiceV2()
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      val request = ValidSubmissionRawRequest.fromCsp
        .withMethod(POST)
        .withHeaders(ValidHeadersV2WithCharset.toSeq: _*)
        .withTarget(ValidSubmissionRawRequest.target.withPath(endpoint))
        .withRawBody(InvalidRawXmlByte)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 400 (BAD_REQUEST) status is received")
      status(result) shouldBe BAD_REQUEST
    }
  }
}
