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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import play.api.test.Helpers._
import util.CustomsDeclarationsExternalServicesConfig.{MdgWcoDecV1ServiceContext, MdgWcoDecV2ServiceContext, MdgWcoDecV3ServiceContext}
import util.{ExternalServicesConfig, WireMockRunner}

trait MdgWcoDecService extends WireMockRunner {
  private val urlV1MatchingRequestPath = urlMatching(MdgWcoDecV1ServiceContext)
  private val urlV2MatchingRequestPath = urlMatching(MdgWcoDecV2ServiceContext)
  private val urlV3MatchingRequestPath = urlMatching(MdgWcoDecV3ServiceContext)

  def startMdgWcoDecServiceV1(): Unit = {
    setupMdgWcoDecServiceToReturn(ACCEPTED, urlV1MatchingRequestPath)
  }

  def startMdgWcoDecServiceV2(): Unit = {
    setupMdgWcoDecServiceToReturn(ACCEPTED, urlV2MatchingRequestPath)
  }

  def startMdgWcoDecServiceV3(): Unit = {
    setupMdgWcoDecServiceToReturn(ACCEPTED, urlV3MatchingRequestPath)
  }


  def setupMdgWcoDecServiceToReturn(status: Int, urlPattern: UrlPattern = urlV2MatchingRequestPath): Unit =
    stubFor(post(urlPattern).
      willReturn(
        aResponse()
          .withStatus(status)))

  def verifyMdgWcoDecServiceWasCalledWithV2(requestBody: String,
                                            expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                            maybeUnexpectedAuthToken: Option[String] = None): Unit = {

    verifyMdgWcoDecServiceWasCalledWith(MdgWcoDecV2ServiceContext, requestBody, expectedAuthToken, maybeUnexpectedAuthToken)
  }

  def verifyMdgWcoDecServiceWasCalledWithV1(requestBody: String,
                                          expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                          maybeUnexpectedAuthToken: Option[String] = None): Unit = {

    verifyMdgWcoDecServiceWasCalledWith(MdgWcoDecV1ServiceContext, requestBody, expectedAuthToken, maybeUnexpectedAuthToken)
  }

  def verifyMdgWcoDecServiceWasCalledWithV3(requestBody: String,
                                          expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                          maybeUnexpectedAuthToken: Option[String] = None): Unit = {

      verifyMdgWcoDecServiceWasCalledWith(MdgWcoDecV3ServiceContext, requestBody, expectedAuthToken, maybeUnexpectedAuthToken)
  }

  def verifyMdgWcoDecServiceWasCalledWith(requestPath: String,
                                          requestBody: String,
                                          expectedAuthToken: String,
                                          maybeUnexpectedAuthToken: Option[String]): Unit = {

    verify(1, postRequestedFor(urlMatching(requestPath))
      .withHeader(CONTENT_TYPE, equalTo(XML + "; charset=utf-8"))
      .withHeader(ACCEPT, equalTo(XML))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $expectedAuthToken"))
      .withHeader(DATE, notMatching(""))
      .withHeader("X-Correlation-ID", notMatching(""))
      .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
      .withRequestBody(equalToXml(requestBody))
    )

    maybeUnexpectedAuthToken foreach { unexpectedAuthToken =>
      verify(0, postRequestedFor(urlMatching(requestPath)).withHeader(AUTHORIZATION, equalTo(s"Bearer $unexpectedAuthToken")))
    }
  }
}
