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

package util

import play.api.http.HeaderNames.{ACCEPT, AUTHORIZATION}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsText, AnyContentAsXml}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, POST}
import util.RequestHeaders._
import util.TestData.{cspBearerToken, nonCspBearerToken}
import util.TestXMLData._

object FakeRequests {
  lazy val ValidSubmissionV2Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidSubmissionV2RequestWithCharSet: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2WithCharset.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidSubmissionV2RequestWithInvalidCharSet: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2WithInvalidCharset.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidSubmissionRawRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)

  lazy val ValidSubmissionV3Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV3.toSeq: _*)
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

  lazy val ValidSubmissionV1Request: FakeRequest[AnyContentAsXml] = ValidSubmissionV2Request
    .withHeaders(ValidSubmissionV2Request.headers.remove(ACCEPT).add(ACCEPT_HMRC_XML_V1_HEADER))

  lazy val ValidSubmissionRequestWithXClientIdHeader: FakeRequest[AnyContentAsXml] = ValidSubmissionV2Request
    .withHeaders(ValidSubmissionV2Request.headers.add(X_CLIENT_ID_HEADER))

  lazy val InvalidSubmissionRequestWithoutXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionV2Request
    .withHeaders(ValidSubmissionV2Request.headers.remove(X_BADGE_IDENTIFIER_NAME))

  lazy val InvalidSubmissionRequestWithInvalidXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionV2Request
    .withHeaders(ValidSubmissionV2Request.headers.remove(X_BADGE_IDENTIFIER_NAME).add(X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG))

  lazy val InvalidSubmissionRequest: FakeRequest[AnyContentAsXml] = ValidSubmissionV2Request.withXmlBody(InvalidSubmissionXML)

  lazy val InvalidSubmissionRequestWith2Errors: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest.withXmlBody(InvalidSubmissionXMLWith2Errors)

  lazy val InvalidCancellationRequestWith2Errors: FakeRequest[AnyContentAsXml] = InvalidCancellationRequest.withXmlBody(InvalidCancellationXMLWith2Errors)

  lazy val MalformedXmlRequest: FakeRequest[AnyContentAsText] = InvalidSubmissionRequest.withTextBody("<xml><non_well_formed></xml>")

  lazy val NoAcceptHeaderSubmissionRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .withHeaders(InvalidSubmissionRequest.headers.remove(ACCEPT))

  lazy val NoAcceptHeaderCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request
    .withHeaders(InvalidSubmissionRequest.headers.remove(ACCEPT))

  lazy val InvalidAcceptHeaderSubmissionRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .withHeaders(RequestHeaders.ACCEPT_HEADER_INVALID)

  lazy val InvalidAcceptHeaderCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request
    .withHeaders(RequestHeaders.ACCEPT_HEADER_INVALID)

  lazy val InvalidContentTypeHeaderSubmissionRequest: FakeRequest[AnyContentAsXml] = InvalidSubmissionRequest
    .withHeaders(ACCEPT_HMRC_XML_V2_HEADER, RequestHeaders.CONTENT_TYPE_HEADER_INVALID)

  lazy val InvalidContentTypeHeaderCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request
    .withHeaders(ACCEPT_HMRC_XML_V2_HEADER, RequestHeaders.CONTENT_TYPE_HEADER_INVALID)

  lazy val NoClientIdIdHeaderSubmissionRequest: FakeRequest[AnyContentAsXml] = ValidSubmissionV2Request
    .withHeaders(InvalidSubmissionRequest.headers.remove(X_CLIENT_ID_NAME))

  lazy val NoClientIdIdHeaderCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request
    .withHeaders(ValidCancellationV2Request.headers.remove(X_CLIENT_ID_NAME))

  lazy val ValidAmendV2Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidAmendV3Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV3.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidArrivalNotificationV2Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidArrivalNotificationV3Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV3.toSeq: _*)
    .withXmlBody(ValidSubmissionXML)

  lazy val ValidCancellationV3Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV3.toSeq: _*)
    .withXmlBody(validCancellationXML())

  lazy val ValidCancellationV2Request: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(validCancellationXML())

  lazy val ValidCancellationRequestWithV1AcceptHeader: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request
    .withHeaders(ValidCancellationV2Request.headers.remove(ACCEPT).add(ACCEPT_HMRC_XML_V1_HEADER))

  lazy val ValidCancellationRequestWithXClientIdHeader: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request
    .withHeaders(ValidCancellationV2Request.headers.add(X_CLIENT_ID_HEADER))

  lazy val InvalidCancellationRequestWithoutXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionV2Request
    .withHeaders(ValidCancellationV2Request.headers.remove(X_BADGE_IDENTIFIER_NAME))

  lazy val InvalidCancellationRequestWithInvalidXBadgeIdentifier: FakeRequest[AnyContentAsXml] = ValidSubmissionV2Request
    .withHeaders(ValidCancellationV2Request.headers.remove(X_BADGE_IDENTIFIER_NAME).add(X_BADGE_IDENTIFIER_HEADER_INVALID_TOO_LONG))

  lazy val InvalidCancellationRequest: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request.withXmlBody(InvalidCancellationXML)

  lazy val ValidFileUploadV2Request = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(validFileUploadXml())

  lazy val ValidFileUploadV3Request = FakeRequest()
    .withHeaders(ValidHeadersV3.toSeq: _*)
    .withXmlBody(validFileUploadXml())

  lazy val InvalidFileUploadRequest = FakeRequest()
    .withHeaders(ValidHeadersV2.toSeq: _*)
    .withXmlBody(InvalidFileUploadXml)

  implicit class FakeRequestOps[R](val fakeRequest: FakeRequest[R]) extends AnyVal {
    def fromCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $cspBearerToken")

    def fromNonCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $nonCspBearerToken")

    def withCustomToken(token: String): FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $token")

    def postTo(endpoint: String): FakeRequest[R] = fakeRequest.withMethod(POST).withTarget(fakeRequest.target.withPath(endpoint))
  }

}
