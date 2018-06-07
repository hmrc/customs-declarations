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

import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsRequest
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http._
import util.CustomsDeclarationsExternalServicesConfig
import util.ExternalServicesConfig.{Host, Port}
import util.TestData.{GoogleAnalyticsPayloadValue, TestCspValidatedPayloadRequest}
import util.externalservices.GoogleAnalyticsService

class GoogleAnalyticsConnectorSpec
  extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
    with BeforeAndAfterAll with GoogleAnalyticsService {

  private lazy val connector = app.injector.instanceOf[GoogleAnalyticsConnector]

  val incomingBearerToken = "some_client's_bearer_token"
  val incomingAuthToken = s"Bearer $incomingBearerToken"
  private val request = GoogleAnalyticsRequest(GoogleAnalyticsPayloadValue)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private implicit val vr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder().configure(Map(
      "auditing.enabled" -> false,
      "microservice.services.google-analytics.host" -> Host,
      "microservice.services.google-analytics.port" -> Port,
      "microservice.services.google-analytics.context" -> CustomsDeclarationsExternalServicesConfig.GoogleAnalyticsContext
    )).build()

  "PublicNotificationServiceConnector" should {

    "make a correct request" in {
      setupServiceToReturn(ACCEPTED)

      await(connector.send(request))

      verifyServiceWasCalledWith(request)
    }

    //    "return a failed future with wrapped HttpVerb NotFoundException when external service returns 404" in {
    //      setupServiceToReturn(NOT_FOUND)
    //
    //      val caught = intercept[RuntimeException](await(connector.send(request)))
    //
    //      caught.getCause.getClass shouldBe classOf[NotFoundException]
    //    }
    //
    //    "return a failed future with wrapped HttpVerbs BadRequestException when external service returns 400" in {
    //      setupServiceToReturn(BAD_REQUEST)
    //
    //      val caught = intercept[RuntimeException](await(connector.send(publicNotificationRequest)))
    //
    //      caught.getCause.getClass shouldBe classOf[BadRequestException]
    //    }
    //
    //    "return a failed future with Upstream5xxResponse when external service returns 500" in {
    //      setupServiceToReturn(INTERNAL_SERVER_ERROR)
    //
    //      intercept[Upstream5xxResponse](await(connector.send(publicNotificationRequest)))
    //    }
    //
    //    "return a failed future with wrapped HttpVerbs BadRequestException when it fails to connect the external service" in
    //      withoutWireMockServer {
    //        val caught = intercept[RuntimeException](await(connector.send(publicNotificationRequest)))
    //
    //        caught.getCause.getClass shouldBe classOf[BadGatewayException]
    //      }
  }

}
