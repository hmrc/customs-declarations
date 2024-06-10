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

package integration.filetransmission

import integration.{IntegrationTestModule, IntegrationTestSpec}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers.{BAD_REQUEST, INTERNAL_SERVER_ERROR, MULTIPLE_CHOICES, NOT_FOUND}
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.filetransmission.FileTransmissionConnector
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.http._
import util.ExternalServicesConfig.{Host, Port}
import util.FileTransmissionTestData._
import util.VerifyLogging._
import util.externalservices.FileTransmissionService
import util.{CustomsDeclarationsExternalServicesConfig, TestData}

class FileTransmissionConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterAll with FileTransmissionService with Matchers {

  private lazy val connector = app.injector.instanceOf[FileTransmissionConnector]
  private implicit val mockCdsLogger: CdsLogger = mock[CdsLogger]

  private implicit val mockDeclarationsLogger: DeclarationsLogger = mock[DeclarationsLogger]
  private implicit val conversationIdRequest: ConversationIdRequest[AnyContentAsXml] = TestData.TestConversationIdRequest

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    reset(mockCdsLogger)
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(IntegrationTestModule(mockDeclarationsLogger).asGuiceableModule)).configure(Map(
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

      val response: Unit = await(sendValidRequest())

      response shouldBe (())
      verifyFileTransmissionServiceWasCalledWith(FileTransmissionRequest)
    }

    "return a failed future when external service returns 300" in {
      setupFileTransmissionToReturn(MULTIPLE_CHOICES)

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[Non2xxResponseException]

      verifyDeclarationsLoggerError("Call to file transmission failed. url=http://localhost:11111/file/transmission, HttpStatus=300, Error=Received a non 2XX response")
    }

    "return a failed future when external service returns 404" in {
      setupFileTransmissionToReturn(NOT_FOUND)

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[Non2xxResponseException]

      verifyDeclarationsLoggerError("Call to file transmission failed. url=http://localhost:11111/file/transmission, HttpStatus=404, Error=Received a non 2XX response")
    }

    "return a failed future when external service returns 400" in {
      setupFileTransmissionToReturn(BAD_REQUEST)

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[Non2xxResponseException]

      verifyDeclarationsLoggerError("Call to file transmission failed. url=http://localhost:11111/file/transmission, HttpStatus=400, Error=Received a non 2XX response")
    }

    "return a failed future when external service returns 500" in {
      setupFileTransmissionToReturn(INTERNAL_SERVER_ERROR)

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[Non2xxResponseException]

      verifyDeclarationsLoggerError("Call to file transmission failed. url=http://localhost:11111/file/transmission, HttpStatus=500, Error=Received a non 2XX response")
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()

      intercept[RuntimeException](await(sendValidRequest())).getCause.getClass shouldBe classOf[BadGatewayException]

      verifyDeclarationsLoggerError(s"Call to file transmission failed. url=http://localhost:11111/file/transmission, HttpStatus=502, Error=POST of 'http://localhost:11111/file/transmission' failed. Caused by: 'Connection refused: localhost/$localhostString:11111'")

      startMockServer()
    }

  }

  private def sendValidRequest() = {
    connector.send(FileTransmissionRequest)
  }
}
