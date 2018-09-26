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
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{BatchFileUploadPayloadValidationAction, BatchFileUploadPayloadValidationComposedAction}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders._
import uk.gov.hmrc.customs.declaration.model.{DocumentationType, _}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.clientId
import util.TestData.{conversationId, nonCspRetrievalValues}
import util.TestXMLData

import scala.concurrent.Future

class BatchFileUploadPayloadValidationComposedActionSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockBatchFileUploadPayloadValidationAction: BatchFileUploadPayloadValidationAction = mock[BatchFileUploadPayloadValidationAction]
    val action: BatchFileUploadPayloadValidationComposedAction = new BatchFileUploadPayloadValidationComposedAction(mockBatchFileUploadPayloadValidationAction, mockLogger)
  }

  "BatchFileUploadPayloadValidationComposedAction" should {

    //TODO remove ignore once refine code in action is implemented
    "return success for valid request" ignore new SetUp {
      val testAr: AuthorisedRequest[AnyContentAsXml] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload,
        VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nonCspRetrievalValues)), FakeRequest("GET", "/").withXmlBody(TestXMLData.ValidBatchFileUploadXml))
      val testVpr: ValidatedPayloadRequest[AnyContentAsXml] = testAr.toValidatedPayloadRequest(TestXMLData.ValidBatchFileUploadXml)

      when(mockBatchFileUploadPayloadValidationAction.refine(testAr)).thenReturn(Future.successful(Right(testVpr)))

      val uploadProperties = List(BatchFileUploadProperties(SequenceNumber(1), DocumentationType("docType1")), BatchFileUploadProperties(SequenceNumber(2), DocumentationType("docType2")))
      val expectedVbfupr: ValidatedBatchFileUploadPayloadRequest[AnyContentAsXml] = testVpr.toValidatedBatchFileUploadPayloadRequest(DeclarationId("decId"), FileGroupSize(2), uploadProperties)
      val result = await(action.refine(testAr))
      result shouldBe Right(expectedVbfupr)
    }
  }
}
