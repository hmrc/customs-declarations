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

import java.util.UUID

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator
import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, AnyContentAsText, AnyContentAsXml}
import play.api.test.FakeRequest
import play.api.test.Helpers.POST
import util.RequestHeaders._
import util.TestData.{cspBearerToken, nonCspBearerToken}
import util.TestXMLData._

object FakeRequests {
  lazy val ValidSubmissionRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidSubmission_13_INV_Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(validSubmissionXML(13, "INV"))

  lazy val ValidSubmission_13_Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(validSubmissionXML(13, "XXA"))

  lazy val ValidSubmission_INV_Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(validSubmissionXML(9, "INV"))

  lazy val ValidSubmissionRequestWithV1AcceptHeader: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers = ValidSubmissionRequest.headers.remove(ACCEPT).add(ACCEPT_HMRC_XML_V1_HEADER))

  lazy val ValidSubmissionRequestWithXClientIdHeader: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.add(X_CLIENT_ID_HEADER))

  lazy val InvalidSubmissionRequestWithoutXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.remove(X_BADGE_IDENTIFIER_NAME))

  lazy val InvalidSubmissionRequestWithInvalidXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidSubmissionRequest.headers.remove(X_BADGE_IDENTIFIER_NAME).add(X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG))

  lazy val InvalidSubmissionRequest: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.withXmlBody(InvalidSubmissionXML)

  lazy val InvalidSubmissionRequestWith3Errors: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest.withXmlBody(InvalidSubmissionXMLWith3Errors)

  lazy val InvalidCancellationRequestWith3Errors: FakeRequest[AnyContentAsXml] = InvalidCancellationRequest.withXmlBody(InvalidCancellationXMLWith3Errors)

  lazy val MalformedXmlRequest: FakeRequest[AnyContentAsText] = InvalidSubmissionRequest.withTextBody("<xml><non_well_formed></xml>")

  lazy val NoAcceptHeaderSubmissionRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .copyFakeRequest(headers = InvalidSubmissionRequest.headers.remove(ACCEPT))

  lazy val NoAcceptHeaderCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationRequest
    .copyFakeRequest(headers = InvalidSubmissionRequest.headers.remove(ACCEPT))

  lazy val InvalidAcceptHeaderSubmissionRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .withHeaders(RequestHeaders.ACCEPT_HEADER_INVALID)

  lazy val InvalidAcceptHeaderCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationRequest
    .withHeaders(RequestHeaders.ACCEPT_HEADER_INVALID)

  lazy val InvalidContentTypeHeaderSubmissionRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .withHeaders(ACCEPT_HMRC_XML_V2_HEADER, RequestHeaders.CONTENT_TYPE_HEADER_INVALID)

  lazy val InvalidContentTypeHeaderCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationRequest
    .withHeaders(ACCEPT_HMRC_XML_V2_HEADER, RequestHeaders.CONTENT_TYPE_HEADER_INVALID)

  lazy val NoClientIdIdHeaderSubmissionRequest: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers = InvalidSubmissionRequest.headers.remove(X_CLIENT_ID_ID_NAME))

  lazy val NoClientIdIdHeaderCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationRequest
    .copyFakeRequest(headers = ValidCancellationRequest.headers.remove(X_CLIENT_ID_ID_NAME))


  lazy val ValidCancellationRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(validCancellationXML())

  lazy val ValidCancellationRequestWithV1AcceptHeader: FakeRequest[AnyContentAsXml] = ValidCancellationRequest
    .copyFakeRequest(headers = ValidCancellationRequest.headers.remove(ACCEPT).add(ACCEPT_HMRC_XML_V1_HEADER))

  lazy val ValidCancellationRequestWithXClientIdHeader: FakeRequest[AnyContentAsXml] = ValidCancellationRequest
    .copyFakeRequest(headers =
      ValidCancellationRequest.headers.add(X_CLIENT_ID_HEADER))

  lazy val InvalidCancellationRequestWithoutXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidCancellationRequest.headers.remove(X_BADGE_IDENTIFIER_NAME))

  lazy val InvalidCancellationRequestWithInvalidXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest
    .copyFakeRequest(headers =
      ValidCancellationRequest.headers.remove(X_BADGE_IDENTIFIER_NAME).add(X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG))

  lazy val InvalidCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationRequest.withXmlBody(InvalidCancellationXML)

  lazy val ValidFileUploadRequest = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(ValidFileUploadXml)

  lazy val ValidFileUploadRequestWithoutBadgeId = ValidFileUploadRequest
    .copyFakeRequest(headers = ValidFileUploadRequest.headers.remove(X_BADGE_IDENTIFIER_NAME))

  lazy val InvalidFileUploadRequest = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(InvalidFileUploadXml)

  lazy val NoAcceptHeaderFileUploadRequest: FakeRequest[AnyContentAsXml] = ValidFileUploadRequest
    .copyFakeRequest(headers = InvalidSubmissionRequest.headers.remove(ACCEPT))

  lazy val InvalidAcceptHeaderFileUploadRequest: FakeRequest[AnyContentAsXml] = ValidFileUploadRequest
    .withHeaders(RequestHeaders.ACCEPT_HEADER_INVALID)

  lazy val InvalidContentTypeHeaderFileUploadRequest: FakeRequest[AnyContentAsXml] = ValidFileUploadRequest
    .withHeaders(ACCEPT_HMRC_XML_V2_HEADER, RequestHeaders.CONTENT_TYPE_HEADER_INVALID)

  implicit class FakeRequestOps[R](val fakeRequest: FakeRequest[R]) extends AnyVal {
    def fromCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $cspBearerToken")

    def fromNonCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $nonCspBearerToken")

    def withCustomToken(token: String): FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $token")

    def postTo(endpoint: String): FakeRequest[R] = fakeRequest.copyFakeRequest(method = POST, uri = endpoint)
  }

}
