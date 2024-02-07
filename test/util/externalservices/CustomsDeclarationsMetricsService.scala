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
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.http.MimeTypes
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers.JSON
import uk.gov.hmrc.customs.declaration.model.CustomsDeclarationsMetricsRequest
import util.{CustomsDeclarationsExternalServicesConfig, WireMockRunner}

trait CustomsDeclarationsMetricsService extends WireMockRunner {
  private val urlMatchingRequestPath = urlMatching(CustomsDeclarationsExternalServicesConfig.CustomsDeclarationsMetricsContext)

  def setupCustomsDeclarationsMetricsServiceToReturn(status: Int = OK): Unit =
    stubFor(post(urlMatchingRequestPath)
      .withHeader(ACCEPT, equalTo(MimeTypes.JSON))
      .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
      willReturn aResponse()
      .withStatus(status))

  def verifyCustomsDeclarationsMetricsServiceWasCalled(): Unit = {
    verify(1, postRequestedFor(urlMatchingRequestPath)
      .withHeader(ACCEPT, equalTo(MimeTypes.JSON))
      .withHeader(CONTENT_TYPE, equalTo(MimeTypes.JSON))
    )
  }

  def verifyCustomsDeclarationsMetricsServiceWasCalledWith(request: CustomsDeclarationsMetricsRequest): Unit = {
    verify(
      1,
      postRequestedFor(urlMatchingRequestPath)
        .withHeader(ACCEPT, equalTo(JSON))
        .withHeader(CONTENT_TYPE, equalTo(JSON))
        .withRequestBody(equalToJson(Json.toJson(request).toString))
    )
  }

}
