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

package unit.controllers

import java.util.UUID

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{contentAsString, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.controllers.BatchFileUploadUpscanNotificationController
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.services.{BatchFileUploadNotificationService, BatchFileUploadUpscanNotificationBusinessService, InternalErrorXmlNotification, UpscanNotificationCallbackToXmlNotification}
import util.ApiSubscriptionFieldsTestData
import util.TestData._
import util.UpscanNotifyTestData._

import scala.concurrent.Future

class BatchFileUploadUpscanNotificationControllerSpec extends PlaySpec with MockitoSugar with Eventually {

  trait Setup {
    val mockNotificationService = mock[BatchFileUploadNotificationService]
    val mockToXmlNotification = mock[UpscanNotificationCallbackToXmlNotification]
    val mockErrorToXmlNotification = mock[InternalErrorXmlNotification]
    val mockBusinessService = mock[BatchFileUploadUpscanNotificationBusinessService]
    val mockCdsLogger = mock[CdsLogger]
    val controller = new BatchFileUploadUpscanNotificationController(
      mockNotificationService,
      mockToXmlNotification,
      mockErrorToXmlNotification,
      mockBusinessService,
      mockCdsLogger)
    val post: Action[AnyContent] = controller.post(ApiSubscriptionFieldsTestData.subscriptionFieldsId.toString)

    def whenNotificationService(callbackBody: UploadedCallbackBody,
                                fileReference: FileReference = FileReferenceOne,
                                csid: SubscriptionFieldsId = ApiSubscriptionFieldsTestData.subscriptionFieldsId,
                                result: Future[Unit] = Future.successful(())): OngoingStubbing[Future[Unit]] = {
      when(mockNotificationService.sendMessage(
        ameq(FailedCallbackBody),
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq[UUID](ApiSubscriptionFieldsTestData.subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId])
      (ameq(mockToXmlNotification))
      ).thenReturn(result)
    }

    def verifyFailureNotificationSent(callbackBody: UploadedFailedCallbackBody,
                                      fileReference: FileReference = FileReferenceOne,
                                      csid: SubscriptionFieldsId = ApiSubscriptionFieldsTestData.subscriptionFieldsId): Future[Unit] = {
      verify(mockNotificationService).sendMessage(
        ameq(callbackBody),
        ameq[UUID](fileReference.value).asInstanceOf[FileReference],
        ameq[UUID](csid.value).asInstanceOf[SubscriptionFieldsId])(ameq(mockToXmlNotification))
    }

    def verifyErrorNotificationSent(fileReference: FileReference = FileReferenceOne,
                                    csid: SubscriptionFieldsId = ApiSubscriptionFieldsTestData.subscriptionFieldsId): Future[Unit] = {
      verify(mockNotificationService).sendMessage(
        ameq(fileReference),
        ameq[UUID](fileReference.value).asInstanceOf[FileReference],
        ameq[UUID](csid.value).asInstanceOf[SubscriptionFieldsId])(ameq(mockErrorToXmlNotification))
    }
  }

  "BatchFileUploadUpscanNotificationController on Happy Path" should {
    "on receipt of READY callback call business service and return 204 with empty body" in new Setup {
      when(mockBusinessService.persistAndCallFileTransmission(ameq(ReadyCallbackBody))(any[HasConversationId])).thenReturn(Future.successful(()))

      private val result: Future[Result] = post(fakeRequestWith(readyJson()))

      Helpers.status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      eventually {
        verifyZeroInteractions(mockNotificationService)
        verify(mockBusinessService).persistAndCallFileTransmission(ameq(ReadyCallbackBody))(any[HasConversationId])
        verifyZeroInteractions(mockErrorToXmlNotification)
      }
    }
  }

  "BatchFileUploadUpscanNotificationController on Unhappy Path" should {
    "on receipt of FAILURE callback send notification and return 204 with empty body" in new Setup {
      whenNotificationService(FailedCallbackBody)

      private val result = post(fakeRequestWith(FailedJson))

      Helpers.status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      eventually {
        verifyFailureNotificationSent(FailedCallbackBody)
        verifyZeroInteractions(mockBusinessService)
        verifyZeroInteractions(mockErrorToXmlNotification)
      }
    }

    "on receipt of FAILURE callback return 500 with standard error message when call to customs notification throws an exception" in new Setup {
      whenNotificationService(FailedCallbackBody, result = Future.failed(emulatedServiceFailure))

      private val result = post(fakeRequestWith(FailedJson))

      Helpers.status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe UpscanNotificationInternalServerErrorJson
      eventually {
        verifyFailureNotificationSent(FailedCallbackBody)
        verifyZeroInteractions(mockBusinessService)
        verifyZeroInteractions(mockErrorToXmlNotification)
      }
    }

    "on receipt of READY callback return 500 with standard error message when business service throw an exception" in new Setup {
      when(mockBusinessService.persistAndCallFileTransmission(ameq(ReadyCallbackBody))(any[HasConversationId]))
        .thenReturn(Future.failed(emulatedServiceFailure))

      private val result = post(fakeRequestWith(readyJson()))

      Helpers.status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe UpscanNotificationInternalServerErrorJson
      eventually {
        verify(mockBusinessService).persistAndCallFileTransmission(ameq(ReadyCallbackBody))(any[HasConversationId])
        verifyErrorNotificationSent()
        verifyZeroInteractions(mockToXmlNotification)
      }
    }

    "return 400 when callback payload is invalid" in new Setup {

      private val result = post(fakeRequestWith(FailedJsonWithInvalidFileStatus))

      Helpers.status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe UpscanNotificationBadRequestJson
      eventually {
        verifyZeroInteractions(mockNotificationService)
        verifyZeroInteractions(mockBusinessService)
        verifyZeroInteractions(mockErrorToXmlNotification)
        verifyZeroInteractions(mockToXmlNotification)
      }
    }
  }

  private def fakeRequestWith(json: JsValue) =
    FakeRequest().withJsonBody(json)

  private implicit val hasConversationId = new HasConversationId {
    override val conversationId: ConversationId = ConversationId(FileReferenceOne.value)
  }

}
