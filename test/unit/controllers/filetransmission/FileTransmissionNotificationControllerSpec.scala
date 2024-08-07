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

package unit.controllers.filetransmission

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.declaration.controllers.filetransmission.FileTransmissionNotificationController
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.model.SubscriptionFieldsId
import uk.gov.hmrc.customs.declaration.model.filetransmission.{FileTransmissionNotification, FileTransmissionSuccessNotification}
import uk.gov.hmrc.customs.declaration.model.upscan.FileReference
import uk.gov.hmrc.customs.declaration.services.filetransmission.FileTransmissionCallbackToXmlNotification
import uk.gov.hmrc.customs.declaration.services.upscan.{CallbackToXmlNotification, FileUploadNotificationService}
import uk.gov.hmrc.http.HeaderCarrier
import util.ApiSubscriptionFieldsTestData.{subscriptionFieldsId, subscriptionFieldsIdString}
import util.FileTransmissionTestData._
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData
import util.TestData.emulatedServiceFailure

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class FileTransmissionNotificationControllerSpec extends PlaySpec
  with MockitoSugar {

  val mockCdsLogger: CdsLogger = mock[CdsLogger]

  trait SetUp {

    implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    val mockService: FileUploadNotificationService = mock[FileUploadNotificationService]
    implicit val callbackToXmlNotification: FileTransmissionCallbackToXmlNotification = mock[FileTransmissionCallbackToXmlNotification]

    val controller = new FileTransmissionNotificationController(callbackToXmlNotification, mockService, Helpers.stubControllerComponents(), mockCdsLogger)
  }

  "file transmission notification controller" should {

    "return 204 when a valid SUCCESS request is received" in new SetUp {
      when(mockService.sendMessage[FileTransmissionNotification](
        ameq(SuccessNotification),
        ameq[UUID](SuccessNotification.fileReference.value).asInstanceOf[FileReference],
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId])(ameq(callbackToXmlNotification), any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val result: Future[Result] = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionSuccessNotificationPayload)))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      verify(mockService).sendMessage(
        ameq(SuccessNotification),
        ameq[UUID](SuccessNotification.fileReference.value).asInstanceOf[FileReference],
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId])(any[CallbackToXmlNotification[FileTransmissionSuccessNotification]], any[HeaderCarrier])
    }

    "return 204 when a valid FAILURE request is received" in new SetUp {
      when(mockService.sendMessage[FileTransmissionNotification](
        ameq(FailureNotification),
        ameq(FailureNotification.fileReference.value).asInstanceOf[FileReference],
        ameq(subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId])(ameq(callbackToXmlNotification), any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val result: Future[Result] = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionFailureNotificationPayload)))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      verify(mockService).sendMessage[FileTransmissionNotification](
        ameq(FailureNotification),
        ameq(FailureNotification.fileReference.value).asInstanceOf[FileReference],
        ameq(subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId])(any[CallbackToXmlNotification[FileTransmissionNotification]], any[HeaderCarrier])
    }

    "return 400 when a invalid request is received" in new SetUp {
      val result: Future[Result] = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(InvalidFileTransmissionNotificationPayload)))

      status(result) mustBe BAD_REQUEST

      contentAsString(result) mustBe BadRequestErrorResponseInvalidOutcome
      verifyNoMoreInteractions(mockService)
    }

    "return 400 when a invalid json is received" in new SetUp {
      val result: Future[Result] = controller.post(subscriptionFieldsIdString)(FakeRequest().withTextBody("some").withHeaders((CONTENT_TYPE, "application/json")))

      status(result) mustBe BAD_REQUEST

      contentAsString(result) mustBe BadRequestErrorResponseInvalidJson
      verifyNoMoreInteractions(mockService)
      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam[String]("Malformed JSON received. Body: Some(some) headers: List((Host,localhost), (Content-Type,application/json))")
        .verify()
    }

    "return 400 when clientSubscriptionId is invalid" in new SetUp {
      val result: Future[Result] = controller.post("invalid-csid")(fakeRequestWith(Json.parse(FileTransmissionSuccessNotificationPayload)))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe FileTransmissionClientSubscriptionIdErrorJson
      verifyNoMoreInteractions(mockService)
      verifyNoMoreInteractions(callbackToXmlNotification)
    }

    "return 500 when call to Custom Notification services fails" in new SetUp {
      when(mockService.sendMessage[FileTransmissionNotification](
        ameq(SuccessNotification),
        ameq[UUID](SuccessNotification.fileReference.value).asInstanceOf[FileReference],
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId])(ameq(callbackToXmlNotification), any[HeaderCarrier]))
        .thenReturn(Future.failed(TestData.emulatedServiceFailure))

      val result: Future[Result] = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionSuccessNotificationPayload)))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe InternalErrorResponseJson
      verify(mockService).sendMessage(
        ameq(SuccessNotification),
        ameq[UUID](SuccessNotification.fileReference.value).asInstanceOf[FileReference],
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId])(any[CallbackToXmlNotification[FileTransmissionSuccessNotification]], any[HeaderCarrier])
      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam[String]("[conversationId=31400000-8ce0-11bd-b23e-10b96e4ef00f][clientSubscriptionId=327d9145-4965-4d28-a2c5-39dedee50334] file transmission notification service request to customs notification failed.")
        .withByNameParam(emulatedServiceFailure)
        .verify()
    }

  }

  private def fakeRequestWith(json: JsValue): FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(json)

}
