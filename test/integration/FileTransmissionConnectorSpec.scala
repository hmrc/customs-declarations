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
import uk.gov.hmrc.customs.declaration.connectors.FileTransmissionConnector
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.http._
import util.CustomsDeclarationsExternalServicesConfig
import util.ExternalServicesConfig.{Host, Port}
import util.FileTransmissionTestData._
import util.TestData._
import util.externalservices.FileTransmissionService

class FileTransmissionConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterAll with FileTransmissionService {

  private lazy val connector = app.injector.instanceOf[FileTransmissionConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

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
      "microservice.services.file-transmission.host" -> Host,
      "microservice.services.file-transmission.port" -> Port,
      "microservice.services.file-transmission.context" -> CustomsDeclarationsExternalServicesConfig.FileTransmissionContext
    )).build()

  "FileTransmissionConnector" should {

    "make a correct request" in {
      startFileTransmissionService()

      val response = await(sendValidRequest())

      response shouldBe (())
      verifyFileTransmissionServiceWasCalledWith(FileTransmissionRequest)
    }

    "return a failed future when external service returns 404" in {
      setupFileTransmissionToReturn(NOT_FOUND)

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[NotFoundException]
    }

    "return a failed future when external service returns 400" in {
      setupFileTransmissionToReturn(BAD_REQUEST)

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[BadRequestException]
    }

    "return a failed future when external service returns 500" in {
      setupFileTransmissionToReturn(INTERNAL_SERVER_ERROR)

      intercept[Upstream5xxResponse](await(sendValidRequest()))
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[BadGatewayException]
      startMockServer()
    }

  }

  private def sendValidRequest() = {
    connector.send(FileTransmissionRequest)
  }
}
