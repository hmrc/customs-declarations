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

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{BatchFileUploadPayloadValidationAction, BatchFileUploadPayloadValidationComposedAction}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.model.{DocumentType, _}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.clientId
import util.TestData.{conversationId, nrsRetrievalValues}
import util.TestXMLData

import scala.concurrent.Future
import scala.xml.Elem

class BatchFileUploadPayloadValidationComposedActionSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockBatchFileUploadPayloadValidationAction: BatchFileUploadPayloadValidationAction = mock[BatchFileUploadPayloadValidationAction]
    val mockDeclarationsConfigService = mock[DeclarationsConfigService]
    when(mockDeclarationsConfigService.batchFileUploadConfig).thenReturn(BatchFileUploadConfig("callback.url", "callback.url", 3, "fileTransmissionCallbackUrl"))
    val action: BatchFileUploadPayloadValidationComposedAction = new BatchFileUploadPayloadValidationComposedAction(mockBatchFileUploadPayloadValidationAction, mockLogger, mockDeclarationsConfigService)
  }

  "BatchFileUploadPayloadValidationComposedAction" should {

    "return 400 when FileGroupSize is greater than config value" in new SetUp {

      private val payload: Elem = TestXMLData.validBatchFileUploadXml()
      val testAr: AuthorisedRequest[AnyContentAsXml] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload,
          VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nrsRetrievalValues)), FakeRequest("GET", "/").withXmlBody(payload))
      val testVpr: ValidatedPayloadRequest[AnyContentAsXml] = testAr.toValidatedPayloadRequest(payload)
      when(mockDeclarationsConfigService.batchFileUploadConfig).thenReturn(BatchFileUploadConfig("callback.url", "callback.url", 1, "fileTransmissionCallbackUrl"))
      when(mockBatchFileUploadPayloadValidationAction.refine(testAr)).thenReturn(Future.successful(Right(testVpr)))

      val result = await(action.refine(testAr))

      val expected = Left(new ErrorResponse(Status.BAD_REQUEST, "BAD_REQUEST", "Payload did not pass validation", ResponseContents("BAD_REQUEST", "FileGroupSize exceeds 3 limit")).XmlResult)
      result shouldBe expected
    }

    "return 400 when FileSequenceNo is greater than FileGroupSize" in new SetUp {

      private val payload: Elem = TestXMLData.validBatchFileUploadXml(fileSequenceNo2 = 3)
      val testAr: AuthorisedRequest[AnyContentAsXml] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload,
        VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nrsRetrievalValues)), FakeRequest("GET", "/").withXmlBody(payload))
      val testVpr: ValidatedPayloadRequest[AnyContentAsXml] = testAr.toValidatedPayloadRequest(payload)
      when(mockBatchFileUploadPayloadValidationAction.refine(testAr)).thenReturn(Future.successful(Right(testVpr)))

      val result = await(action.refine(testAr))

      val expected = Left(new ErrorResponse(Status.BAD_REQUEST, "BAD_REQUEST", "Payload did not pass validation", ResponseContents("BAD_REQUEST", "FileSequenceNo must not be greater than FileGroupSize")).XmlResult)
      result shouldBe expected
    }

    "return 400 when number of file elements does not match FileGroupSize" in new SetUp {

      private val payload: Elem = TestXMLData.validBatchFileUploadXml(1, 1, 1)
      val testAr: AuthorisedRequest[AnyContentAsXml] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload,
        VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nrsRetrievalValues)), FakeRequest("GET", "/").withXmlBody(payload))
      val testVpr: ValidatedPayloadRequest[AnyContentAsXml] = testAr.toValidatedPayloadRequest(payload)
      when(mockBatchFileUploadPayloadValidationAction.refine(testAr)).thenReturn(Future.successful(Right(testVpr)))

      val result = await(action.refine(testAr))

      val expected = Left(new ErrorResponse(Status.BAD_REQUEST, "BAD_REQUEST", "Payload did not pass validation", ResponseContents("BAD_REQUEST", "FileGroupSize does not match number of File elements"), ResponseContents("BAD_REQUEST", "FileSequenceNo contains duplicates")).XmlResult)
      result shouldBe expected
    }

    "return 400 when FileSequenceNo is duplicated" in new SetUp {

      private val payload: Elem = TestXMLData.validBatchFileUploadXml(2, 1, 1)
      val testAr: AuthorisedRequest[AnyContentAsXml] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload,
        VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nrsRetrievalValues)), FakeRequest("GET", "/").withXmlBody(payload))
      val testVpr: ValidatedPayloadRequest[AnyContentAsXml] = testAr.toValidatedPayloadRequest(payload)
      when(mockBatchFileUploadPayloadValidationAction.refine(testAr)).thenReturn(Future.successful(Right(testVpr)))

      val result = await(action.refine(testAr))

      val expected = Left(new ErrorResponse(Status.BAD_REQUEST, "BAD_REQUEST", "Payload did not pass validation", ResponseContents("BAD_REQUEST", "FileSequenceNo contains duplicates")).XmlResult)
      result shouldBe expected
    }

    "return 400 when FileSequenceNo does not start from 1" in new SetUp {

      private val payload: Elem = TestXMLData.validBatchFileUploadXml(2, 0, 1)
      val testAr: AuthorisedRequest[AnyContentAsXml] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload,
        VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nrsRetrievalValues)), FakeRequest("GET", "/").withXmlBody(payload))
      val testVpr: ValidatedPayloadRequest[AnyContentAsXml] = testAr.toValidatedPayloadRequest(payload)
      when(mockBatchFileUploadPayloadValidationAction.refine(testAr)).thenReturn(Future.successful(Right(testVpr)))

      val result = await(action.refine(testAr))

      val expected = Left(new ErrorResponse(Status.BAD_REQUEST, "BAD_REQUEST", "Payload did not pass validation", ResponseContents("BAD_REQUEST", "FileSequenceNo must start from 1")).XmlResult)
      result shouldBe expected
    }

    "return success for valid request" in new SetUp {

      private val payload: Elem = TestXMLData.validBatchFileUploadXml()
      val testAr: AuthorisedRequest[AnyContentAsXml] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload,
        VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nrsRetrievalValues)), FakeRequest("GET", "/").withXmlBody(payload))
      val testVpr: ValidatedPayloadRequest[AnyContentAsXml] = testAr.toValidatedPayloadRequest(payload)
      when(mockBatchFileUploadPayloadValidationAction.refine(testAr)).thenReturn(Future.successful(Right(testVpr)))
      val expected = Right(testVpr.toValidatedBatchFileUploadPayloadRequest(
        BatchFileUploadRequest(DeclarationId("declarationId"),
          FileGroupSize(2),
          Seq(
            BatchFileUploadFile(FileSequenceNo(1), DocumentType("document type 1")),
            BatchFileUploadFile(FileSequenceNo(2), DocumentType("document type 2"))
          )
        )
      ))

      val result = await(action.refine(testAr))

      result shouldBe expected
    }
  }
}
