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
import com.github.tomakehurst.wiremock.matching.UrlPattern
import play.api.test.Helpers._
import util.{CustomsDeclarationsExternalServicesConfig, ExternalServicesConfig, TestXMLData, WireMockRunner}

import scala.xml.NodeSeq

trait MdgStatusDeclarationService extends WireMockRunner {
  private val v2URL = urlMatching(CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContextV2)
  private val v3URL = urlMatching(CustomsDeclarationsExternalServicesConfig.MdgStatusDeclarationServiceContextV3)

  def startMdgStatusV2Service(status: Int = OK, body: NodeSeq = TestXMLData.validStatusResponse()): Unit = startService(status, v2URL, body)

  def startMdgStatusV3Service(status: Int = OK, body: NodeSeq = TestXMLData.validStatusResponse()): Unit = startService(status, v3URL, body)

  private def startService (status: Int, url: UrlPattern, body: NodeSeq) = {
    stubFor(post(url).
      willReturn(
        aResponse()
          .withStatus(status)
          .withBody(body.toString())))
  }

  def verifyMdgStatusDecServiceWasCalledWith(requestBody: String,
                                          expectedAuthToken: String = ExternalServicesConfig.AuthToken,
                                          maybeUnexpectedAuthToken: Option[String] = None) {
    verify(1, postRequestedFor(v2URL)
      .withHeader(CONTENT_TYPE, equalTo(XML + "; charset=utf-8"))
      .withHeader(ACCEPT, equalTo(XML))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $expectedAuthToken"))
      .withHeader(DATE, notMatching(""))
      .withHeader("X-Correlation-ID", notMatching(""))
      .withHeader(X_FORWARDED_HOST, equalTo("MDTP"))
      .withRequestBody(equalToXml(requestBody))
      )

    maybeUnexpectedAuthToken foreach { unexpectedAuthToken =>
      verify(0, postRequestedFor(v2URL).withHeader(AUTHORIZATION, equalTo(s"Bearer $unexpectedAuthToken")))
    }
  }
}
