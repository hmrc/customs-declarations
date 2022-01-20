/*
 * Copyright 2022 HM Revenue & Customs
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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.Helpers.OK
import uk.gov.hmrc.customs.declaration.connectors.ApiSubscriptionFieldsPath.url
import uk.gov.hmrc.customs.declaration.model.ApiSubscriptionKey
import util.{ApiSubscriptionFieldsTestData, CustomsDeclarationsExternalServicesConfig, WireMockRunner}

trait ApiSubscriptionFieldsService extends WireMockRunner with ApiSubscriptionFieldsTestData {
  private def apiSubsUrl(apiSubsKey: ApiSubscriptionKey) = url(CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext, apiSubsKey)
  private def urlMatchingRequestPath(apiSubs: ApiSubscriptionKey) = {
    urlEqualTo(apiSubsUrl(apiSubs))
  }

  def startApiSubscriptionFieldsService(apiSubsKey: ApiSubscriptionKey): Unit =
    setupGetSubscriptionFieldsToReturn(OK, apiSubsKey)

  def setupGetSubscriptionFieldsToReturn(status: Int = OK, apiSubsKey: ApiSubscriptionKey = apiSubscriptionKey): Unit = {
    stubFor(get(urlMatchingRequestPath(apiSubsKey: ApiSubscriptionKey)).
      willReturn(
        aResponse()
          .withBody(responseJsonString)
          .withStatus(status))
    )
  }

  def verifyGetSubscriptionFieldsCalled(apiSubsKey: ApiSubscriptionKey = apiSubscriptionKey): Unit = {
    verify(1, getRequestedFor(urlMatchingRequestPath(apiSubsKey: ApiSubscriptionKey)))
  }

  def verifyGetSubscriptionFieldsNotCalled(apiSubsKey: ApiSubscriptionKey = apiSubscriptionKey): Unit = {
    verify(0, getRequestedFor(urlMatchingRequestPath(apiSubsKey: ApiSubscriptionKey)))
  }
}
