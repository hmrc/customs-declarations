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

package integration

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.customs.declaration.connectors.DeclarationStatusConnector
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AuthorisedStatusRequest
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import util.CustomsDeclarationsMetricsTestData._
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.TestData._
import util._
import util.externalservices.MdgStatusDeclarationService

class DeclarationStatusConnectorSpec extends IntegrationTestSpec
  with GuiceOneAppPerSuite
  with MockitoSugar
  with BeforeAndAfterAll
  with MdgStatusDeclarationService {

  private lazy val connector = app.injector.instanceOf[DeclarationStatusConnector]

  private val incomingAuthToken = s"Bearer ${ExternalServicesConfig.AuthToken}"
  private val numberOfCallsToTriggerStateChange = 5
  private val unavailablePeriodDurationInMillis = 1000
  private implicit val asr: AuthorisedStatusRequest[AnyContent] = AuthorisedStatusRequest(conversationId, GoogleAnalyticsValues.DeclarationStatus, EventStart, VersionTwo, badgeIdentifier, ApiSubscriptionFieldsTestData.clientId, mock[Request[AnyContent]])
  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(incomingAuthToken)))

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
      verifyMdgStatusDecServiceWasCalledWith(requestBody = StatusTestXMLData.expectedDeclarationStatusPayload.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "circuit breaker trips after specified number of failures" in {
      startMdgStatusV2Service(INTERNAL_SERVER_ERROR)

      1 to numberOfCallsToTriggerStateChange foreach { _ =>
        val k = intercept[Upstream5xxResponse](await(sendValidXml()))
        k.reportAs shouldBe BAD_GATEWAY
      }

      1 to 3 foreach { _ =>
        val k = intercept[UnhealthyServiceException](await(sendValidXml()))
        k.getMessage shouldBe "declaration-status"
      }

      resetMockServer()
      startMdgStatusV2Service(ACCEPTED)

      Thread.sleep(unavailablePeriodDurationInMillis)

      1 to 5 foreach { _ =>
        resetMockServer()
        startMdgStatusV2Service(ACCEPTED)
        await(sendValidXml())
        verifyMdgStatusDecServiceWasCalledWith(requestBody = StatusTestXMLData.expectedDeclarationStatusPayload.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
      }
    }

    "return a failed future when external service returns 404" in {
      startMdgStatusV2Service(NOT_FOUND)
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[NotFoundException]
    }

    "return a failed future when external service returns 400" in {
      startMdgStatusV2Service(BAD_REQUEST)
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[BadRequestException]
    }

    "return a failed future when external service returns 500" in {
      startMdgStatusV2Service(INTERNAL_SERVER_ERROR)
      intercept[Upstream5xxResponse](await(sendValidXml()))
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[BadGatewayException]
      startMockServer()
    }
  }

  private def sendValidXml() = {
    connector.send(date, correlationId, dmirId, VersionTwo, mrn)
  }
}
