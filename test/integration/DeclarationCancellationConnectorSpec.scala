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

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.connectors.DeclarationCancellationConnector
import uk.gov.hmrc.customs.declaration.http.Non2xxResponseException
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.http._
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.TestData._
import util.TestXMLData.ValidSubmissionXML
import util.externalservices.MdgCancellationDeclarationService
import util.{CustomsDeclarationsExternalServicesConfig, TestData}

import java.time.Instant
import java.util.UUID

class DeclarationCancellationConnectorSpec extends IntegrationTestSpec
  with Matchers
  with GuiceOneAppPerSuite
  with MockitoSugar
  with BeforeAndAfterAll
  with MdgCancellationDeclarationService {

  private lazy val connector = app.injector.instanceOf[DeclarationCancellationConnector]

  private val incomingBearerToken = "some_client's_bearer_token"
  private val incomingAuthToken = s"Bearer $incomingBearerToken"
  private val correlationId = UUID.randomUUID()
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def beforeAll(): Unit = {
    startMockServer()
  }

  override protected def beforeEach(): Unit = {
    when(mockUuidService.uuid()).thenReturn(correlationId)
  }

  override protected def afterEach(): Unit = {
    resetMockServer()
  }

  override protected def afterAll(): Unit = {
    stopMockServer()
  }

  override implicit lazy val app: Application =
    GuiceApplicationBuilder(overrides = Seq(TestModule.asGuiceableModule)).configure(Map(
      "microservice.services.declaration-cancellation.host" -> Host,
      "microservice.services.declaration-cancellation.port" -> Port,
      "microservice.services.declaration-cancellation.context" -> CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContext,
      "microservice.services.declaration-cancellation.bearer-token" -> AuthToken
    )).build()

  "DeclarationCancellationConnector" should {

    "make a correct request" in {
      startMdgCancellationV1Service(ACCEPTED)
      await(sendValidXml())
      verifyMdgWcoDecServiceWasCalledWithV1(requestBody = ValidSubmissionXML.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "return a failed future when external service returns 404" in {
      startMdgCancellationV1Service(NOT_FOUND)
      checkCaughtException(NOT_FOUND)
    }

    "return a failed future when external service returns 400" in {
      startMdgCancellationV1Service(BAD_REQUEST)
      checkCaughtException(BAD_REQUEST)
    }

    "return a failed future when external service returns 500" in {
      startMdgCancellationV1Service(INTERNAL_SERVER_ERROR)
      checkCaughtException(INTERNAL_SERVER_ERROR)
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[BadGatewayException](await(sendValidXml()))
      startMockServer()
    }
  }

  private def sendValidXml()(implicit vpr: ValidatedPayloadRequest[_]) = {
    connector.send(ValidSubmissionXML, Instant.now(), correlationId, VersionOne)
  }

  private def checkCaughtException(status: Int): Unit = {
    val exception = intercept[Non2xxResponseException](await(sendValidXml()))
    exception.responseCode shouldBe status
  }
}
