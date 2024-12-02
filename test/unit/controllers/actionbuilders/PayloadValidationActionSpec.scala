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

package unit.controllers.actionbuilders

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.PayloadValidationAction
import uk.gov.hmrc.customs.declaration.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper.*
import uk.gov.hmrc.customs.declaration.model.actionbuilders.*
import uk.gov.hmrc.customs.declaration.model.{Csp, VersionOne}
import uk.gov.hmrc.customs.declaration.services.XmlValidationService
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData.*

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.SAXException

class PayloadValidationActionSpec extends AnyWordSpecLike with MockitoSugar with Matchers{

  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val forConversions: ConversationIdRequest[AnyContentAsXml] = TestConversationIdRequest
  private val saxException = new SAXException("Boom!")
  private val expectedXmlSchemaErrorResult = ErrorResponse
    .errorBadRequest("Payload is not valid according to schema")
    .withErrors(ResponseContents("xml_validation_error", saxException.getMessage)).XmlResult.withConversationId

  trait SetUp {
    val mockXmlValidationService: XmlValidationService = mock[XmlValidationService]
    val mockExportsLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val payloadValidationAction: PayloadValidationAction = new PayloadValidationAction(mockXmlValidationService, mockExportsLogger){
      override def executionContext: ExecutionContext = Helpers.stubControllerComponents().executionContext
    }
  }
  "PayloadValidationAction" should {
    "return a ValidatedPayloadRequest when XML validation is OK" in new SetUp {
      when(mockXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.successful(()))

      private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadValidationAction.refine(TestCspAuthorisedRequest))

      actual shouldBe Right(TestCspValidatedPayloadRequest)
    }

    "return 400 error response when XML is not well formed" in new SetUp {
      when(mockXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(saxException))

      private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadValidationAction.refine(TestCspAuthorisedRequest))

      actual shouldBe Left(expectedXmlSchemaErrorResult)
    }

    "return 400 error response when XML validation fails" in new SetUp {
      private val errorMessage = "Request body does not contain a well-formed XML document."
      private val errorNotWellFormed = ErrorResponse.errorBadRequest(errorMessage).XmlResult.withConversationId
      private val authorisedRequestWithNonWellFormedXml = ApiVersionRequest(conversationId, EventStart, VersionOne, FakeRequest().withTextBody("<foo><foo>"))
        .toValidatedHeadersRequest(TestExtractedHeaders).toCspAuthorisedRequest(Csp(None, Some(badgeIdentifier), Some(nrsRetrievalValues)))

      private val actual = await(payloadValidationAction.refine(authorisedRequestWithNonWellFormedXml))

      actual shouldBe Left(errorNotWellFormed)
    }

    "propagates downstream errors by returning a 500 error response" in new SetUp {
      when(mockXmlValidationService.validate(TestCspValidatedPayloadRequest.body.asXml.get)).thenReturn(Future.failed(emulatedServiceFailure))

      private val actual: Either[Result, ValidatedPayloadRequest[AnyContentAsXml]] = await(payloadValidationAction.refine(TestCspAuthorisedRequest))

      actual shouldBe Left(ErrorResponse.ErrorInternalServerError.XmlResult.withConversationId)
      PassByNameVerifier(mockExportsLogger, "debug")
        .withByNameParam[String](s"Error validating payload.:\n<foo>bar</foo>")
        .withByNameParam(emulatedServiceFailure)
        .withParamMatcher(any[HasConversationId])
        .verify()
    }
  }

}
