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
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.JsValue
import util.{CustomsDeclarationsExternalServicesConfig, WireMockRunner}

trait GoogleAnalyticsService extends WireMockRunner {
  private val urlMatchingRequestPath = urlMatching(CustomsDeclarationsExternalServicesConfig.GoogleAnalyticsContext)

  def setupGoogleAnalyticsServiceToReturn(status: Int): Unit =
    stubFor(post(urlMatchingRequestPath)
      .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.JSON))
      .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
      willReturn aResponse()
      .withStatus(status))

  def verifyGoogleAnalyticsServiceWasCalled {
    verify(1, postRequestedFor(urlMatchingRequestPath)
      .withHeader(HeaderNames.ACCEPT, equalTo(MimeTypes.JSON))
      .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
    )
  }

  def verifyGoogleAnalyticsServiceWasNotCalled {
    verify(0, postRequestedFor(urlMatchingRequestPath)
    )
  }

}
