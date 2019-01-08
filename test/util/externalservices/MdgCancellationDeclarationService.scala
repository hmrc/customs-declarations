/*
 * Copyright 2019 HM Revenue & Customs
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
import util.{CustomsDeclarationsExternalServicesConfig, ExternalServicesConfig, WireMockRunner}

trait MdgCancellationDeclarationService extends WireMockRunner {
  private val v1URL = urlMatching(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContext)
  private val v2URL = urlMatching(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContextV2)
  private val v3URL = urlMatching(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContextV3)

  def startMdgCancellationV1Service(status: Int = ACCEPTED): Unit = startService(status, v1URL)

  def startMdgCancellationV2Service(status: Int = ACCEPTED): Unit = startService(status, v2URL)

  def startMdgCancellationV3Service(status: Int = ACCEPTED): Unit = startService(status, v3URL)

  private def startService (status: Int, url: UrlPattern) = {
    stubFor(post(url).
      willReturn(
        aResponse()
          .withStatus(status)))

  }
  def verifyMdgWcoDecServiceWasCalledWith(requestBody: String,
                                          expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                          maybeUnexpectedAuthToken: Option[String] = None) {
    verify(1, postRequestedFor(v1URL)
      .withHeader(CONTENT_TYPE, equalTo(XML))
      .withHeader(ACCEPT, equalTo(XML))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $expectedAuthToken"))
      .withHeader(DATE, notMatching(""))
      .withHeader("X-Correlation-ID", notMatching(""))
      .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
      .withRequestBody(equalToXml(requestBody))
      )

    maybeUnexpectedAuthToken foreach { unexpectedAuthToken =>
      verify(0, postRequestedFor(v1URL).withHeader(AUTHORIZATION, equalTo(s"Bearer $unexpectedAuthToken")))
    }
  }
}
