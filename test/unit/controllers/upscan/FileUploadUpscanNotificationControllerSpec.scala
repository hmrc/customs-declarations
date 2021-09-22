/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.controllers.upscan

import java.util.UUID

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{contentAsString, _}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declaration.controllers.upscan.FileUploadUpscanNotificationController
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.model.upscan.{FileReference, UploadedCallbackBody, UploadedFailedCallbackBody}
import uk.gov.hmrc.customs.declaration.services.InternalErrorXmlNotification
import uk.gov.hmrc.customs.declaration.services.upscan.{FileUploadNotificationService, FileUploadUpscanNotificationBusinessService, UpscanNotificationCallbackToXmlNotification}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import unit.logging.StubCdsLogger
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.TestData._
import util.UpscanNotifyTestData._

import scala.concurrent.Future

class FileUploadUpscanNotificationControllerSpec extends PlaySpec with MockitoSugar with Eventually {

  trait SetUp {
    implicit val ec = Helpers.stubControllerComponents().executionContext
    val mockNotificationService: FileUploadNotificationService = mock[FileUploadNotificationService]
    val mockToXmlNotification: UpscanNotificationCallbackToXmlNotification = mock[UpscanNotificationCallbackToXmlNotification]
    val mockErrorToXmlNotification: InternalErrorXmlNotification = mock[InternalErrorXmlNotification]
    val mockBusinessService: FileUploadUpscanNotificationBusinessService = mock[FileUploadUpscanNotificationBusinessService]
    val controller = new FileUploadUpscanNotificationController(
      mockNotificationService,
      mockToXmlNotification,
      mockErrorToXmlNotification,
      mockBusinessService,
      Helpers.stubControllerComponents(),
      new StubCdsLogger(mock[ServicesConfig]))
    val post: Action[AnyContent] = controller.post(subscriptionFieldsId.toString)
    val postWithInvalidCsid: Action[AnyContent] = controller.post("invalid-csid")

    def whenNotificationService(callbackBody: UploadedCallbackBody,
                                fileReference: FileReference = FileReferenceOne,
                                csid: SubscriptionFieldsId = subscriptionFieldsId,
                                result: Future[Unit] = Future.successful(())): OngoingStubbing[Future[Unit]] = {
      when(mockNotificationService.sendMessage(
        ameq(FailedCallbackBody),
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId])
      (ameq(mockToXmlNotification))
      ).thenReturn(result)
    }

    def verifyFailureNotificationSent(callbackBody: UploadedFailedCallbackBody,
                                      fileReference: FileReference = FileReferenceOne,
                                      csid: SubscriptionFieldsId = subscriptionFieldsId): Future[Unit] = {
      verify(mockNotificationService).sendMessage(
        ameq(callbackBody),
        ameq[UUID](fileReference.value).asInstanceOf[FileReference],
        ameq[UUID](csid.value).asInstanceOf[SubscriptionFieldsId])(ameq(mockToXmlNotification))
    }

    def verifyErrorNotificationSent(fileReference: FileReference = FileReferenceOne,
                                    csid: SubscriptionFieldsId = subscriptionFieldsId): Future[Unit] = {
      verify(mockNotificationService).sendMessage(
        ameq(fileReference),
        ameq[UUID](fileReference.value).asInstanceOf[FileReference],
        ameq[UUID](csid.value).asInstanceOf[SubscriptionFieldsId])(ameq(mockErrorToXmlNotification))
    }
  }

  "FileUploadUpscanNotificationController on Happy Path" should {
    "on receipt of READY callback call business service and return 204 with empty body" in new SetUp {
      when(mockBusinessService.persistAndCallFileTransmission(ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId], ameq(ReadyCallbackBody))(any[HasConversationId])).thenReturn(Future.successful(()))

      private val result = post(fakeRequestWith(readyJson()))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      eventually {
        verifyNoInteractions(mockNotificationService)
        verify(mockBusinessService).persistAndCallFileTransmission(ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId], ameq(ReadyCallbackBody))(any[HasConversationId])
        verifyNoInteractions(mockErrorToXmlNotification)
      }
    }
  }

  "FileUploadUpscanNotificationController on Unhappy Path" should {
    "on receipt of FAILURE callback send notification and return 204 with empty body" in new SetUp {
      whenNotificationService(FailedCallbackBody)

      private val result = post(fakeRequestWith(FailedJson))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      eventually {
        verifyFailureNotificationSent(FailedCallbackBody)
        verifyNoInteractions(mockBusinessService)
        verifyNoInteractions(mockErrorToXmlNotification)
      }
    }

    "on receipt of FAILURE callback return 500 with standard error message when call to customs notification throws an exception" in new SetUp {
      whenNotificationService(FailedCallbackBody, result = Future.failed(emulatedServiceFailure))

      private val result = post(fakeRequestWith(FailedJson))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe UpscanNotificationInternalServerErrorJson
      eventually {
        verifyFailureNotificationSent(FailedCallbackBody)
        verifyNoInteractions(mockBusinessService)
        verifyNoInteractions(mockErrorToXmlNotification)
      }
    }

    "on receipt of READY callback return 500 with standard error message when business service throw an exception" in new SetUp {
      when(mockBusinessService.persistAndCallFileTransmission(ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId], ameq(ReadyCallbackBody))(any[HasConversationId]))
        .thenReturn(Future.failed(emulatedServiceFailure))

      private val result = post(fakeRequestWith(readyJson()))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe UpscanNotificationInternalServerErrorJson
      eventually {
        verify(mockBusinessService).persistAndCallFileTransmission(ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId], ameq(ReadyCallbackBody))(any[HasConversationId])
        verifyErrorNotificationSent()
        verifyNoInteractions(mockToXmlNotification)
      }
    }

    "return 400 when clientSubscriptionId is invalid" in new SetUp {
      private val result = postWithInvalidCsid(fakeRequestWith(readyJson()))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe UpscanNotifyClientSubscriptionIdErrorJson
      eventually {
        verifyNoInteractions(mockNotificationService)
        verifyNoInteractions(mockBusinessService)
        verifyNoInteractions(mockErrorToXmlNotification)
        verifyNoInteractions(mockToXmlNotification)
      }
    }

    "return 400 when callback payload is invalid" in new SetUp {

      private val result = post(fakeRequestWith(FailedJsonWithInvalidFileStatus))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe UpscanNotificationBadRequestJson
      eventually {
        verifyNoInteractions(mockNotificationService)
        verifyNoInteractions(mockBusinessService)
        verifyNoInteractions(mockErrorToXmlNotification)
        verifyNoInteractions(mockToXmlNotification)
      }
    }

    "return 400 when a invalid json is received" in new SetUp {
    private val result = post(FakeRequest().withTextBody("some").withHeaders((CONTENT_TYPE, "application/json")))

    status(result) mustBe BAD_REQUEST

    contentAsString(result) mustBe UpscanNotificationBadRequestJsonPayload
    verifyNoInteractions(mockNotificationService)
    verifyNoInteractions(mockBusinessService)
    verifyNoInteractions(mockErrorToXmlNotification)
    verifyNoInteractions(mockToXmlNotification)
  }

}

  private def fakeRequestWith(json: JsValue) =
    FakeRequest().withJsonBody(json)
}
