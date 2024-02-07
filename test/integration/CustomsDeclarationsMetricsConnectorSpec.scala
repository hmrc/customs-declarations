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

package integration

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.customs.declaration.connectors.CustomsDeclarationsMetricsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import util.CustomsDeclarationsMetricsTestData._
import util.ExternalServicesConfig.{Host, Port}
import util.VerifyLogging.verifyDeclarationsLoggerError
import util.externalservices.{AuditService, CustomsDeclarationsMetricsService}
import util.{CustomsDeclarationsExternalServicesConfig, TestData}


class CustomsDeclarationsMetricsConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
with BeforeAndAfterAll with AuditService with CustomsDeclarationsMetricsService with Matchers {

  private lazy val connector = app.injector.instanceOf[CustomsDeclarationsMetricsConnector]

  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest
  private implicit val mockDeclarationsLogger: DeclarationsLogger = mock[DeclarationsLogger]
  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    resetMockServer()
    setupAuditServiceToReturn()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(IntegrationTestModule(mockDeclarationsLogger).asGuiceableModule)).configure(Map(
      "auditing.consumer.baseUri.host" -> Host,
      "auditing.consumer.baseUri.port" -> Port,
      "auditing.enabled" -> true,
      "microservice.services.customs-declarations-metrics.host" -> Host,
      "microservice.services.customs-declarations-metrics.port" -> Port,
      "microservice.services.customs-declarations-metrics.context" -> CustomsDeclarationsExternalServicesConfig.CustomsDeclarationsMetricsContext
    )).build()

  "MetricsConnector" should {

    "make a correct request" in {
      setupCustomsDeclarationsMetricsServiceToReturn()

      val response: Unit = sendValidRequest()

      response shouldBe (() : Unit)
      verifyCustomsDeclarationsMetricsServiceWasCalledWith(ValidCustomsDeclarationsMetricsRequest)
      verifyAuditServiceWasNotCalled()
    }

    "return a failed future when external service returns 404" in {
      setupCustomsDeclarationsMetricsServiceToReturn(NOT_FOUND)

      (sendValidRequest()).futureValue shouldBe (())
      verifyAuditServiceWasNotCalled()
      verifyDeclarationsLoggerError("Call to customs declarations metrics service failed. url=http://localhost:11111/log-times, HttpStatus=404, Error=Received a non 2XX response, response body=")
    }

    "return a failed future when external service returns 400" in {
      setupCustomsDeclarationsMetricsServiceToReturn(BAD_REQUEST)

      (sendValidRequest()).futureValue shouldBe (())
      verifyAuditServiceWasNotCalled()
      verifyDeclarationsLoggerError("Call to customs declarations metrics service failed. url=http://localhost:11111/log-times, HttpStatus=400, Error=Received a non 2XX response, response body=")
    }

    "return a failed future when external service returns 500" in {
      setupCustomsDeclarationsMetricsServiceToReturn(INTERNAL_SERVER_ERROR)

      (sendValidRequest()).futureValue shouldBe (())
      verifyAuditServiceWasNotCalled()
      verifyDeclarationsLoggerError("Call to customs declarations metrics service failed. url=http://localhost:11111/log-times, HttpStatus=500, Error=Received a non 2XX response, response body=")
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()

      intercept[RuntimeException]((sendValidRequest()).futureValue).getCause.getClass shouldBe classOf[RuntimeException]

      verifyDeclarationsLoggerError(s"Call to customs declarations metrics service failed. url=http://localhost:11111/log-times, HttpStatus=502, Error=POST of 'http://localhost:11111/log-times' failed. Caused by: 'Connection refused: localhost/$localhostString:11111'")

      startMockServer()
    }
  }

  private def sendValidRequest() = {
    connector.post(ValidCustomsDeclarationsMetricsRequest)
  }
}


