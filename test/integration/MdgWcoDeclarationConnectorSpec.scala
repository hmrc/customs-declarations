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

import java.util.UUID

import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.connectors.MdgWcoDeclarationConnector
import uk.gov.hmrc.customs.declaration.model.{ConversationId, Ids}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import util.ApiSubscriptionFieldsTestData.fieldsIdString
import util.ExternalServicesConfig.{AuthToken, Host, Port}
import util.TestData._
import util.externalservices.MdgWcoDecService
import util.{CustomsDeclarationsExternalServicesConfig, RequestHeaders}

class MdgWcoDeclarationConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar
  with BeforeAndAfterAll with MdgWcoDecService {

  private lazy val connector = app.injector.instanceOf[MdgWcoDeclarationConnector]

  private val incomingBearerToken = "some_client's_bearer_token"
  private val incomingAuthToken = s"Bearer $incomingBearerToken"
  private val correlationId = UUID.randomUUID()
  private implicit val ids: Ids = Ids(ConversationId("dummy-conversation-id"))

  private implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(incomingAuthToken)))
    .withExtraHeaders(RequestHeaders.API_SUBSCRIPTION_FIELDS_ID_NAME -> fieldsIdString)

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
      "auditing.consumer.baseUri.host" -> Host,
      "auditing.consumer.baseUri.port" -> Port,
      "auditing.enabled" -> false,
      "microservice.services.wco-declaration.host" -> Host,
      "microservice.services.wco-declaration.port" -> Port,
      "microservice.services.wco-declaration.context" -> CustomsDeclarationsExternalServicesConfig.MdgWcoDecV2ServiceContext,
      "microservice.services.wco-declaration.bearer-token" -> AuthToken
    )).build()

  "MdgWcoDeclarationConnector" should {

    "make a correct request" in {
      setupMdgWcoDecServiceToReturn(NO_CONTENT)
      await(sendValidXml())
      verifyMdgWcoDecServiceWasCalledWith(requestBody = ValidXML.toString(), maybeUnexpectedAuthToken = Some(incomingAuthToken))
    }

    "return a failed future when external service returns 404" in {
      setupMdgWcoDecServiceToReturn(NOT_FOUND)
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[NotFoundException]
    }

    "return a failed future when external service returns 400" in {
      setupMdgWcoDecServiceToReturn(BAD_REQUEST)
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[BadRequestException]
    }

    "return a failed future when external service returns 500" in {
      setupMdgWcoDecServiceToReturn(INTERNAL_SERVER_ERROR)
      intercept[Upstream5xxResponse](await(sendValidXml()))
    }

    "return a failed future when fail to connect the external service" in {
      stopMockServer()
      intercept[RuntimeException](await(sendValidXml())).getCause.getClass shouldBe classOf[BadGatewayException]
      startMockServer()
    }

  }

  private def sendValidXml()(implicit ids: Ids) = {
    connector.send(ValidXML, new DateTime(), correlationId, None)
  }
}
