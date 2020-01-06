/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.customs.declaration.connectors.MdgDeclarationCancellationConnector
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.TestData._
import util.TestXMLData.ValidSubmissionXML
import util.externalservices.MdgCancellationDeclarationService
import util.{CustomsDeclarationsExternalServicesConfig, TestData}

class DeclarationCancellationConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterAll with MdgCancellationDeclarationService {

  private lazy val connector = app.injector.instanceOf[MdgDeclarationCancellationConnector]

  private val incomingBearerToken = "some_client's_bearer_token"
  private val incomingAuthToken = s"Bearer $incomingBearerToken"
  private val numberOfCallsToTriggerStateChange = 5
  private val unavailablePeriodDurationInMillis = 1000
  private val correlationId = UUID.randomUUID()
  private implicit val vpr = TestData.TestCspValidatedPayloadRequest

  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(incomingAuthToken)))

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    when(mockUuidService.uuid()).thenReturn(correlationId)
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.declaration-cancellation.host" -> Host,
      "microservice.services.declaration-cancellation.port" -> Port,
      "microservice.services.declaration-cancellation.context" -> CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContext,
      "microservice.services.declaration-cancellation.bearer-token" -> AuthToken
    )).build()

  "MdgWcoDeclarationConnector" should {

    "make a correct request" in {
      startMdgCancellationV1Service(ACCEPTED)
      await(sendValidXml())
      verifyMdgWcoDecServiceWasCalledWithV1(requestBody = ValidSubmissionXML.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "circuit breaker trips after specified number of failures" in {
      startMdgCancellationV1Service(INTERNAL_SERVER_ERROR)

      1 to numberOfCallsToTriggerStateChange foreach { _ =>
        val k = intercept[Upstream5xxResponse](await(sendValidXml()))
        k.reportAs shouldBe BAD_GATEWAY
      }

      1 to 3 foreach { _ =>
        val k = intercept[UnhealthyServiceException](await(sendValidXml()))
        k.getMessage shouldBe "declaration-cancellation"
      }

      resetMockServer()
      startMdgCancellationV1Service(ACCEPTED)

      Thread.sleep(unavailablePeriodDurationInMillis)

      1 to 5 foreach { _ =>
        resetMockServer()
        startMdgCancellationV1Service(ACCEPTED)
        await(sendValidXml())
        verifyMdgWcoDecServiceWasCalledWithV1(requestBody = ValidSubmissionXML.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
      }
    }

    "return a failed future when external service returns 404" in {
      startMdgCancellationV1Service(NOT_FOUND)
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[NotFoundException]
    }

    "return a failed future when external service returns 400" in {
      startMdgCancellationV1Service(BAD_REQUEST)
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[BadRequestException]
    }

    "return a failed future when external service returns 500" in {
      startMdgCancellationV1Service(INTERNAL_SERVER_ERROR)
      intercept[Upstream5xxResponse](await(sendValidXml()))
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[BadGatewayException]
      startMockServer()
    }
  }

  private def sendValidXml()(implicit vpr: ValidatedPayloadRequest[_]) = {
    connector.send(ValidSubmissionXML, new DateTime(), correlationId, VersionOne)
  }
}
