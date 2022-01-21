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
import com.github.tomakehurst.wiremock.matching.UrlPattern
import play.api.test.Helpers._
import util.CustomsDeclarationsExternalServicesConfig.{MdgCancellationDeclarationServiceContext, MdgCancellationDeclarationServiceContextV2, MdgCancellationDeclarationServiceContextV3}
import util.{ExternalServicesConfig, WireMockRunner}

trait MdgCancellationDeclarationService extends WireMockRunner {
  private val v1URL = urlMatching(MdgCancellationDeclarationServiceContext)
  private val v2URL = urlMatching(MdgCancellationDeclarationServiceContextV2)
  private val v3URL = urlMatching(MdgCancellationDeclarationServiceContextV3)

  def startMdgCancellationV1Service(status: Int = ACCEPTED): Unit = startService(status, v1URL)

  def startMdgCancellationV2Service(status: Int = ACCEPTED): Unit = startService(status, v2URL)

  def startMdgCancellationV3Service(status: Int = ACCEPTED): Unit = startService(status, v3URL)

  private def startService (status: Int, url: UrlPattern) = {
    stubFor(post(url).
      willReturn(
        aResponse()
          .withStatus(status)))

  }
  def verifyMdgWcoDecServiceWasCalledWithV1(requestBody: String,
                                            expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                            maybeUnexpectedAuthToken: Option[String] = None) {

    verifyMdgWcoDecServiceWasCalledWith(MdgCancellationDeclarationServiceContext, requestBody, expectedAuthToken, maybeUnexpectedAuthToken)
  }

  def verifyMdgWcoDecServiceWasCalledWithV2(requestBody: String,
                                          expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                          maybeUnexpectedAuthToken: Option[String] = None) {

    verifyMdgWcoDecServiceWasCalledWith(MdgCancellationDeclarationServiceContextV2, requestBody, expectedAuthToken, maybeUnexpectedAuthToken)
  }

  private def verifyMdgWcoDecServiceWasCalledWith(requestPath: String,
                                                  requestBody: String,
                                                  expectedAuthToken: String ,
                                                  maybeUnexpectedAuthToken: Option[String]) {
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
