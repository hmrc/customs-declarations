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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.test.Helpers._
import util.{ExternalServicesConfig, CustomsDeclarationsExternalServicesConfig, WireMockRunner}

trait MdgWcoDecService extends WireMockRunner {
  private val urlV1MatchingRequestPath = urlMatching(CustomsDeclarationsExternalServicesConfig.MdgWcoDecV1ServiceContext)
  private val urlV2MatchingRequestPath = urlMatching(CustomsDeclarationsExternalServicesConfig.MdgWcoDecV2ServiceContext)

  def startMdgWcoDecService(): Unit = {
    setupMdgWcoDecServiceToReturn(ACCEPTED)

    stubFor(post(urlV1MatchingRequestPath).
      willReturn(
        aResponse()
          .withStatus(ACCEPTED)))
  }

  def setupMdgWcoDecServiceToReturn(status: Int): Unit =
    stubFor(post(urlV2MatchingRequestPath).
      willReturn(
        aResponse()
          .withStatus(status)))

  def verifyMdgWcoDecServiceWasCalledWith(requestBody: String,
                                          expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                          maybeUnexpectedAuthToken: Option[String] = None) {
    verify(1, postRequestedFor(urlV2MatchingRequestPath)
      .withHeader(CONTENT_TYPE, equalTo(XML))
      .withHeader(ACCEPT, equalTo(XML))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $expectedAuthToken"))
      .withHeader(DATE, notMatching(""))
      .withHeader("X-Correlation-ID", notMatching(""))
      .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
      .withRequestBody(equalToXml(requestBody))
      )

    maybeUnexpectedAuthToken foreach { unexpectedAuthToken =>
      verify(0, postRequestedFor(urlV2MatchingRequestPath).withHeader(AUTHORIZATION, equalTo(s"Bearer $unexpectedAuthToken")))
    }
  }
}
