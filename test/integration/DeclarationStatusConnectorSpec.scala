/*
 * Copyright 2023 HM Revenue & Customs
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

package integration

import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.http._
import util.CustomsDeclarationsMetricsTestData._
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.StatusTestXMLData.expectedDeclarationStatusPayload
import util.TestData._
import util._
import util.externalservices.MdgStatusDeclarationService

class DeclarationStatusConnectorSpec extends IntegrationTestSpec
  with Matchers
  with GuiceOneAppPerSuite
  with MockitoSugar
  with BeforeAndAfterAll
  with MdgStatusDeclarationService {

  private lazy val connector = app.injector.instanceOf[DeclarationStatusConnector]

  private val incomingAuthToken = s"Bearer ${ExternalServicesConfig.AuthToken}"
  private implicit val ar: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, EventStart, VersionTwo, ApiSubscriptionFieldsTestData.clientId, Csp(None, Some(badgeIdentifier), None), mock[Request[AnyContent]])

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    when(mockUuidService.uuid()).thenReturn(correlationIdUuid)
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.v2.declaration-status.host" -> Host,
      "microservice.services.v2.declaration-status.port" -> Port,
      "microservice.services.v2.declaration-status.context" -> CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContextV2,
      "microservice.services.v2.declaration-status.bearer-token" -> AuthToken
    )).build()

  "DeclarationStatusConnector" should {

    "make a correct request" in {
      startMdgStatusV2Service()
      await(sendValidXml())
      verifyMdgStatusDecServiceWasCalledWith(requestBody = expectedDeclarationStatusPayload.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "return a failed future when external service returns 404" in {
      startMdgStatusV2Service(NOT_FOUND)
      checkCaughtException(NOT_FOUND)
    }

    "return a failed future when external service returns 400" in {
      startMdgStatusV2Service(BAD_REQUEST)
      checkCaughtException(BAD_REQUEST)
    }

    "return a failed future when external service returns 500" in {
      startMdgStatusV2Service(INTERNAL_SERVER_ERROR)
      checkCaughtException(INTERNAL_SERVER_ERROR)
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[BadGatewayException](await(sendValidXml()))
      startMockServer()
    }
  }

  private def sendValidXml() = {
    connector.send(expectedDeclarationStatusPayload, date, correlationId, VersionTwo)
  }

  private def checkCaughtException(status: Int) {
    val exception = intercept[Non2xxResponseException](await(sendValidXml()))
    exception.responseCode shouldBe status
  }
}
