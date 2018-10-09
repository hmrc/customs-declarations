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
import play.api.test.Helpers.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.customs.declaration.connectors.BatchFileUploadCustomsNotificationConnector
import uk.gov.hmrc.customs.declaration.services.BatchFileUploadCustomsNotification
import uk.gov.hmrc.http._
import util.ExternalServicesConfig.{Host, Port}
import util.TestData._
import util.externalservices.CustomsNotificationService
import util.{ApiSubscriptionFieldsTestData, CustomsDeclarationsExternalServicesConfig, TestData}

class BatchFileUploadNotificationConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterAll with CustomsNotificationService {

  private lazy val connector = app.injector.instanceOf[BatchFileUploadCustomsNotificationConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val xml = <foo>bar</foo>
  private val notification =
    BatchFileUploadCustomsNotification(ApiSubscriptionFieldsTestData.subscriptionFieldsId, TestData.conversationId.uuid, xml)

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
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "auditing.consumer.baseUri.host" -> Host,
      "auditing.consumer.baseUri.port" -> Port,
      "auditing.enabled" -> false,
      "microservice.services.customs-notification.host" -> Host,
      "microservice.services.customs-notification.port" -> Port,
      "microservice.services.customs-notification.context" -> CustomsDeclarationsExternalServicesConfig.CustomsNotificationContext,
      "microservice.services.customs-notification.bearer-token" -> CustomsDeclarationsExternalServicesConfig.CustomsNotificationAuthHeaderValue
    )).build()

  "BatchFileUploadCustomsNotificationConnector" should {

    "make a correct request" in {
      notificationServiceIsRunning()

      val response = awaitSendValidRequest()

      response shouldBe (())
      verifyWasCalledWith(xml.toString)
    }

    "return a failed future when external service returns 404" in {
      setupCustomsNotificationToReturn(NOT_FOUND)

      intercept[RuntimeException](awaitSendValidRequest()).getCause.getClass shouldBe classOf[NotFoundException]
    }

    "return a failed future when external service returns 400" in {
      setupCustomsNotificationToReturn(BAD_REQUEST)

      intercept[RuntimeException](awaitSendValidRequest()).getCause.getClass shouldBe classOf[BadRequestException]
    }

    "return a failed future when external service returns 500" in {
      setupCustomsNotificationToReturn(INTERNAL_SERVER_ERROR)

      intercept[Upstream5xxResponse](awaitSendValidRequest())
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()

      intercept[RuntimeException](awaitSendValidRequest()).getCause.getClass shouldBe classOf[BadGatewayException]
      startMockServer()
    }

  }

  private def awaitSendValidRequest() = {
    await(connector.send(notification))
  }
}
