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

package unit.controllers.actionbuilders

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.PayloadValidationAction
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.model.{Csp, GoogleAnalyticsValues}
import uk.gov.hmrc.customs.declaration.services.XmlValidationService
import uk.gov.hmrc.play.test.UnitSpec
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.SAXException

class PayloadValidationActionSpec extends UnitSpec with MockitoSugar {

  private implicit val forConversions: AnalyticsValuesAndConversationIdRequest[AnyContentAsXml] = TestConversationIdRequest
  private val saxException = new SAXException("Boom!")
  private val expectedXmlSchemaErrorResult = ErrorResponse
    .errorBadRequest("Payload is not valid according to schema")
    .withErrors(ResponseContents("xml_validation_error", saxException.getMessage)).XmlResult.withConversationId

  trait SetUp {
    val mockXmlValidationService: XmlValidationService = mock[XmlValidationService]
    val mockExportsLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockGoogleAnalyticsConnector: GoogleAnalyticsConnector = mock[GoogleAnalyticsConnector]
    val payloadValidationAction: PayloadValidationAction = new PayloadValidationAction(mockXmlValidationService, mockExportsLogger, Some(mockGoogleAnalyticsConnector)){}
  }
  "PayloadValidationAction" should {
    "return a ValidatedPayloadRequest when XML validation is OK" in new SetUp {
      when(mockXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.successful(()))

      private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadValidationAction.refine(TestCspAuthorisedRequest))

      actual shouldBe Right(TestCspValidatedPayloadRequest)
    }

    "return 400 error response when XML is not well formed  (and send GA request)" in new SetUp {
      when(mockXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(saxException))

      private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadValidationAction.refine(TestCspAuthorisedRequest))

      actual shouldBe Left(expectedXmlSchemaErrorResult)
      verify(mockGoogleAnalyticsConnector).failure("Payload did not pass validation against the schema.")(TestCspAuthorisedRequest)
    }

    "return 400 error response when XML validation fails (and send GA request)" in new SetUp {
      private val errorMessage = "Request body does not contain a well-formed XML document."
      private val errorNotWellFormed = ErrorResponse.errorBadRequest(errorMessage).XmlResult.withConversationId
      private val authorisedRequestWithNonWellFormedXml = AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, FakeRequest().withTextBody("<foo><foo>"))
        .toValidatedHeadersRequest(TestExtractedHeaders).toCspAuthorisedRequest(Csp(badgeIdentifier, Some(nrsRetrievalValues)))

      private val actual = await(payloadValidationAction.refine(authorisedRequestWithNonWellFormedXml))

      actual shouldBe Left(errorNotWellFormed)
      verify(mockGoogleAnalyticsConnector).failure(errorMessage)(authorisedRequestWithNonWellFormedXml)
    }

    "propagates downstream errors by returning a 500 error response (and doesn't send GA request)" in new SetUp {
      when(mockXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(emulatedServiceFailure))

      private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadValidationAction.refine(TestCspAuthorisedRequest))

      actual shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
      verifyZeroInteractions(mockGoogleAnalyticsConnector)
    }
  }

}
