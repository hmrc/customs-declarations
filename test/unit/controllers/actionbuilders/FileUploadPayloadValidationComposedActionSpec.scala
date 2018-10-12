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
import play.api.mvc._
import play.api.test.FakeRequest
import play.mvc.Http.Status.FORBIDDEN
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{FileUploadPayloadValidationAction, FileUploadPayloadValidationComposedAction}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedPayloadRequest, ValidatedUploadPayloadRequest}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.clientId
import util.RequestHeaders
import util.TestData._

import scala.concurrent.Future
import scala.xml.NodeSeq

class FileUploadPayloadValidationComposedActionSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockFileUploadPayloadValidationAction: FileUploadPayloadValidationAction = mock[FileUploadPayloadValidationAction]
    val fileUploadPayloadValidationComposedAction: FileUploadPayloadValidationComposedAction = new FileUploadPayloadValidationComposedAction(mockFileUploadPayloadValidationAction, mockLogger)
  }

  "FileUploadPayloadValidationComposedAction" should  {
    "return 403 response when authorised as CSP" in new SetUp {
      val authorisedCspRequest: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload, VersionTwo, clientId, Csp(BadgeIdentifier("CSP1"), Some(nrsRetrievalValues)), mock[Request[AnyContent]])

      val actualResult: Either[Result, ValidatedUploadPayloadRequest[AnyContent]] = await(fileUploadPayloadValidationComposedAction.refine(authorisedCspRequest))

      actualResult shouldBe Left(ErrorResponse(FORBIDDEN, ForbiddenCode, "Not an authorized service").XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationIdValue))
    }

    "return an error when validation fails" in new SetUp {
      val authorisedNonCspRequest: AuthorisedRequest[AnyContent] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload, VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nrsRetrievalValues)), mock[Request[AnyContent]])
      val mockResult: Result = mock[Result]

      when(mockFileUploadPayloadValidationAction.refine(authorisedNonCspRequest)).thenReturn(Future.successful(Left(mockResult)))

      await(fileUploadPayloadValidationComposedAction.refine(authorisedNonCspRequest)) shouldBe Left(mockResult)
    }

    "return success when there are no errors" in new SetUp {
      val testUpscanInitiatePayload: NodeSeq = <upscanInitiate><declarationID>dec123</declarationID><documentationType>docType123</documentationType></upscanInitiate>
      val testAr: AuthorisedRequest[AnyContentAsXml] = AuthorisedRequest(conversationId, GoogleAnalyticsValues.Fileupload, VersionTwo, clientId, NonCsp(Eori("EORI123"), Some(nrsRetrievalValues)), FakeRequest("GET", "/").withXmlBody(testUpscanInitiatePayload))
      val testVpr: ValidatedPayloadRequest[AnyContentAsXml] = testAr.toValidatedPayloadRequest(testUpscanInitiatePayload)

      when(mockFileUploadPayloadValidationAction.refine(testAr)).thenReturn(Future.successful(Right(testVpr)))

      val expectedVupr: ValidatedUploadPayloadRequest[AnyContentAsXml] = testVpr.toValidatedUploadPayloadRequest(DeclarationId("dec123"), DocumentationType("docType123"))
      await(fileUploadPayloadValidationComposedAction.refine(testAr)) shouldBe Right(expectedVupr)
    }
  }
}
