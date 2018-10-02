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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.test.Helpers._
import util.CustomsDeclarationsExternalServicesConfig.UpscanInitiateContext
import util.WireMockRunner

trait UpscanInitiateService extends WireMockRunner {
  private val urlMatchingRequestPath = urlMatching(UpscanInitiateContext)

  private val validUpscanInitiateResponse = s"""{
    "reference": "${UUID.randomUUID()}",
    "uploadRequest": {
      "href": "https://bucketName.s3.eu-west-2.amazonaws.com",
      "fields": {
      "X-Amz-Algorithm": "AWS4-HMAC-SHA256",
      "X-Amz-Expiration": "2018-02-09T12:35:45.297Z",
      "X-Amz-Signature": "xxxx",
      "key": "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "acl": "private",
      "X-Amz-Credential": "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
      "policy": "xxxxxxxx=="
    }
    }
  }""".stripMargin

  def startUpscanInitiateService(): Unit = {
    setupUpscanInitiateToReturn(OK)
  }

  def setupUpscanInitiateToReturn(status: Int): StubMapping = stubFor(post(urlMatchingRequestPath).
    willReturn(
      aResponse().withBody(validUpscanInitiateResponse)
        .withStatus(status)))

  def verifyUpscanInitiateServiceWasCalled() {
    verify(1, postRequestedFor(urlMatchingRequestPath))
  }
}
