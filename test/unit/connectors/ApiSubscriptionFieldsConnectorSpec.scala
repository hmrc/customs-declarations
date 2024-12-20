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

package unit.connectors

import org.mockito.ArgumentMatchers.{any, eq as ameq}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import util.CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext
import util.ExternalServicesConfig.*
import util.{ApiSubscriptionFieldsTestData, TestData}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class ApiSubscriptionFieldsConnectorSpec extends AnyWordSpecLike
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach
  with Eventually
  with ApiSubscriptionFieldsTestData {

  private val mockWSGetImpl = mock[HttpClient]
  private val mockLogger = mock[DeclarationsLogger]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val mockDeclarationsConfig = mock[DeclarationsConfig]
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext

  private val connector = new ApiSubscriptionFieldsConnector(mockWSGetImpl, mockLogger, mockDeclarationsConfigService)

  private val expectedUrl = s"http://$Host:$Port$ApiSubscriptionFieldsContext/application/SOME_X_CLIENT_ID/context/some/api/context/version/1.0"
  private val url = new URL(expectedUrl)
  override protected def beforeEach(): Unit = {
    reset(mockLogger, mockWSGetImpl, mockDeclarationsConfigService)

    when(mockDeclarationsConfigService.declarationsConfig).thenReturn(mockDeclarationsConfig)
    when(mockDeclarationsConfig.apiSubscriptionFieldsBaseUrl).thenReturn(s"http://$Host:$Port$ApiSubscriptionFieldsContext")
  }

  "ApiSubscriptionFieldsConnector" can {
    "when making a successful request" should {
      "use the correct URL for valid path parameters and config" in {
        val futureResponse = Future.successful(apiSubscriptionFieldsResponse)
        when(mockWSGetImpl.GET[ApiSubscriptionFieldsResponse](
            ameq(url))
          (any[HttpReads[ApiSubscriptionFieldsResponse]](), any[HeaderCarrier](), any[ExecutionContext])).thenReturn(futureResponse)

        awaitRequest shouldBe apiSubscriptionFieldsResponse
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when api subscription fields call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))

        val caught = intercept[TestData.EmulatedServiceFailure] {
          awaitRequest
        }

        caught shouldBe TestData.emulatedServiceFailure
      }
    }
  }

  private def awaitRequest = {
    await(connector.getSubscriptionFields(apiSubscriptionKey))
  }

  private def returnResponseForRequest(eventualResponse: Future[ApiSubscriptionFieldsResponse]) = {
    when(mockWSGetImpl.GET[ApiSubscriptionFieldsResponse](ameq(url))
      (any[HttpReads[ApiSubscriptionFieldsResponse]](), any[HeaderCarrier](), any[ExecutionContext])).thenReturn(eventualResponse)
  }
}
