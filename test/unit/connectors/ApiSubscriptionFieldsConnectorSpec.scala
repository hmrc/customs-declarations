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

package unit.connectors

import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionFieldsResponse, ApiSubscriptionKey}
import uk.gov.hmrc.customs.declaration.services.WSHttp
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, NotFoundException}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec
import util.ExternalServicesConfig._
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext
import util.{ApiSubscriptionFieldsTestData, TestData}

import scala.concurrent.{ExecutionContext, Future}

class ApiSubscriptionFieldsConnectorSpec extends UnitSpec
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with ApiSubscriptionFieldsTestData {

  private val mockWSGetImpl = mock[WSHttp]
  private val mockDeclarationsLogger = mock[DeclarationsLogger]
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector = connectorWithConfig(validConfig)

  private val httpException = new NotFoundException("Emulated 404 response from a web call")
  private val expectedUrl = s"http://$Host:$Port$ApiSubscriptionFieldsContext/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0"

  private val expectedApiSubscriptionKey = ApiSubscriptionKey(xClientId, "customs%2Fdeclarations", "2.0")

  override protected def beforeEach() {
    reset(mockDeclarationsLogger, mockWSGetImpl)
  }

  "ApiSubscriptionFieldsConnector" can {
    "when making a successful request" should {
      "use the correct URL for valid path parameters and config" in {
        val futureResponse = Future.successful(apiSubscriptionFieldsResponse)
        when(mockWSGetImpl.GET[ApiSubscriptionFieldsResponse](
          ameq(expectedUrl))
          (any[HttpReads[ApiSubscriptionFieldsResponse]](), any[HeaderCarrier](), any[ExecutionContext])).thenReturn(futureResponse)

        awaitRequest shouldBe apiSubscriptionFieldsResponse
      }
    }

    "when configuration is invalid" should {
      "throw RuntimeException when host is missing" in {
        val caught = intercept[RuntimeException] {
          await(connectorWithConfig(invalidConfigMissingHost).getSubscriptionFields(expectedApiSubscriptionKey))
        }

        caught.getMessage shouldBe "Could not find config api-subscription-fields.host"
      }

      "throw RuntimeException when context is missing" in {
        val caught = intercept[IllegalStateException] {
          await(connectorWithConfig(invalidConfigMissingContext).getSubscriptionFields(expectedApiSubscriptionKey))
        }

        caught.getMessage shouldBe "Configuration error - api-subscription-fields.context not found."
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when api subscription fields call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))

        val caught = intercept[TestData.EmulatedServiceFailure] {
          awaitRequest
        }

        caught shouldBe TestData.emulatedServiceFailure
        eventuallyVerifyError(caught)
      }

      "wrap an underlying error when api subscription fields call fails with an http exception" in {
        returnResponseForRequest(Future.failed(httpException))

        val caught = intercept[RuntimeException] {
          awaitRequest
        }

        caught.getCause shouldBe httpException
        eventuallyVerifyError(caught)
      }
    }
  }

  private def awaitRequest = {
    await(connector.getSubscriptionFields(apiSubscriptionKey))
  }

  private def returnResponseForRequest(eventualResponse: Future[ApiSubscriptionFieldsResponse]) = {
    when(mockWSGetImpl.GET[ApiSubscriptionFieldsResponse](anyString())
      (any[HttpReads[ApiSubscriptionFieldsResponse]](), any[HeaderCarrier](), any[ExecutionContext])).thenReturn(eventualResponse)
  }

  private def eventuallyVerifyError(caught: Throwable) {
    eventually {
      PassByNameVerifier(mockDeclarationsLogger, "error")
        .withByNameParam[String](s"Call to subscription information service failed. url=$expectedUrl")
        .withByNameParam[Throwable](caught)
        .withAnyHeaderCarrierParam()
        .verify()
    }
  }

  private def testServicesConfig(configuration: Config) = new ServicesConfig {
    override val runModeConfiguration = new Configuration(configuration)
    override val mode = play.api.Mode.Test
    override def environment: Environment = mock[Environment]
  }

  private def connectorWithConfig(config: Config) = new ApiSubscriptionFieldsConnector(mockWSGetImpl, mockDeclarationsLogger, testServicesConfig(config))

}
