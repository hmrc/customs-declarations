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

package util

import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.mvc.{AnyContentAsText, AnyContentAsXml}
import play.api.test.FakeRequest
import play.api.test.Helpers.POST
import util.RequestHeaders._
import util.TestData.{cspBearerToken, nonCspBearerToken}
import util.TestXMLData._

object FakeRequests {
  lazy val ValidSubmissionRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeaders.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidSubmissionRequestWithV1AcceptHeader: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers = ValidSubmissionRequest.headers.remove(ACCEPT).add(ACCEPT_HMRC_XML_V1_HEADER))

  lazy val ValidSubmissionRequestWithXClientIdHeader: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.remove(API_SUBSCRIPTION_FIELDS_ID_NAME).add(X_CLIENT_ID_HEADER))

  lazy val InvalidSubmissionRequestWithoutXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.remove(X_BADGE_IDENTIFIER_NAME))

  lazy val InvalidSubmissionRequestWithInvalidXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.remove(X_BADGE_IDENTIFIER_NAME).add(X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG))

  lazy val InvalidSubmissionRequest: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.withXmlBody(InvalidSubmissionXML)

  lazy val InvalidRequestWith3Errors: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest.withXmlBody(InvalidSubmissionXMLWith3Errors)

  lazy val MalformedXmlRequest: FakeRequest[AnyContentAsText] = InvalidSubmissionRequest.withTextBody("<xml><non_well_formed></xml>")

  lazy val NoAcceptHeaderRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .copyFakeRequest(headers = InvalidSubmissionRequest.headers.remove(ACCEPT))

  lazy val InvalidAcceptHeaderRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .withHeaders(RequestHeaders.ACCEPT_HEADER_INVALID)

  lazy val InvalidContentTypeHeaderRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .withHeaders(ACCEPT_HMRC_XML_V2_HEADER, RequestHeaders.CONTENT_TYPE_HEADER_INVALID)

  lazy val NoClientIdIdHeaderRequest: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers = InvalidSubmissionRequest.headers.remove(API_SUBSCRIPTION_FIELDS_ID_NAME))


  lazy val ValidCancellationRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeaders.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidCancellationRequestWithV1AcceptHeader: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers = ValidSubmissionRequest.headers.remove(ACCEPT).add(ACCEPT_HMRC_XML_V1_HEADER))

  lazy val ValidCancellationRequestWithXClientIdHeader: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.remove(API_SUBSCRIPTION_FIELDS_ID_NAME).add(X_CLIENT_ID_HEADER))

  lazy val InvalidCancellationRequestWithoutXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.remove(X_BADGE_IDENTIFIER_NAME))

  lazy val InvalidCancellationRequestWithInvalidXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.remove(X_BADGE_IDENTIFIER_NAME).add(X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG))

  lazy val InvalidCancellationRequest: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.withXmlBody(InvalidSubmissionXML)

  implicit class FakeRequestOps[R](val fakeRequest: FakeRequest[R]) extends AnyVal {
    def fromCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $cspBearerToken")
    def fromNonCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $nonCspBearerToken")
    def postTo(endpoint: String): FakeRequest[R] = fakeRequest.copyFakeRequest(method = POST, uri = endpoint)
  }
}
