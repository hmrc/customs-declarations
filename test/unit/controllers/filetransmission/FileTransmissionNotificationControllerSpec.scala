/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.controllers.filetransmission.FileTransmissionNotificationController
import uk.gov.hmrc.customs.declaration.model.filetransmission.FileTransmissionNotification
import uk.gov.hmrc.customs.declaration.services.filetransmission.FileTransmissionCallbackToXmlNotification
import uk.gov.hmrc.customs.declaration.services.upscan.FileUploadNotificationService
import util.ApiSubscriptionFieldsTestData.{subscriptionFieldsId, subscriptionFieldsIdString}
import util.FileTransmissionTestData._
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData
import util.TestData.emulatedServiceFailure

import scala.concurrent.Future

class FileTransmissionNotificationControllerSpec extends PlaySpec
  with MockitoSugar {

  val mockCdsLogger = mock[CdsLogger]

  trait SetUp {

    implicit val ec = Helpers.stubControllerComponents().executionContext
    val mockService = mock[FileUploadNotificationService]
    implicit val callbackToXmlNotification = mock[FileTransmissionCallbackToXmlNotification]

    val controller = new FileTransmissionNotificationController(callbackToXmlNotification, mockService, Helpers.stubControllerComponents(), mockCdsLogger)
  }

  "file transmission notification controller" should {

    "return 204 when a valid SUCCESS request is received" in new SetUp {
      when(mockService.sendMessage[FileTransmissionNotification](SuccessNotification, SuccessNotification.fileReference, subscriptionFieldsId)(callbackToXmlNotification)).thenReturn(Future.successful(()))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionSuccessNotificationPayload)))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      verify(mockService).sendMessage[FileTransmissionNotification](SuccessNotification, SuccessNotification.fileReference, subscriptionFieldsId)
    }

    "return 204 when a valid FAILURE request is received" in new SetUp {
      when(mockService.sendMessage[FileTransmissionNotification](FailureNotification, FailureNotification.fileReference, subscriptionFieldsId)(callbackToXmlNotification)).thenReturn(Future.successful(()))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionFailureNotificationPayload)))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      verify(mockService).sendMessage[FileTransmissionNotification](FailureNotification, FailureNotification.fileReference, subscriptionFieldsId)
    }

    "return 400 when a invalid request is received" in new SetUp {
      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(InvalidFileTransmissionNotificationPayload)))

      status(result) mustBe BAD_REQUEST

      contentAsString(result) mustBe BadRequestErrorResponseInvalidOutcome
      verifyNoInteractions(mockService)
    }

    "return 400 when a invalid json is received" in new SetUp {
      val result = controller.post(subscriptionFieldsIdString)(FakeRequest().withTextBody("some").withHeaders((CONTENT_TYPE, "application/json")))

      status(result) mustBe BAD_REQUEST

      contentAsString(result) mustBe BadRequestErrorResponseInvalidJson
      verifyNoInteractions(mockService)
      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam[String]("Malformed JSON received. Body: Some(some) headers: List((Host,localhost), (Content-Type,application/json))")
        .verify()
    }

    "return 400 when clientSubscriptionId is invalid" in new SetUp {
      val result = controller.post("invalid-csid")(fakeRequestWith(Json.parse(FileTransmissionSuccessNotificationPayload)))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe FileTransmissionClientSubscriptionIdErrorJson
      verifyNoInteractions(mockService)
      verifyNoInteractions(callbackToXmlNotification)
    }

    "return 500 when call to Custom Notification services fails" in new SetUp {
      when(mockService.sendMessage[FileTransmissionNotification](SuccessNotification, SuccessNotification.fileReference, subscriptionFieldsId)(callbackToXmlNotification)).thenReturn(Future.failed(TestData.emulatedServiceFailure))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionSuccessNotificationPayload)))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe InternalErrorResponseJson
      verify(mockService).sendMessage[FileTransmissionNotification](SuccessNotification, SuccessNotification.fileReference, subscriptionFieldsId)(callbackToXmlNotification)
      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam[String]("[conversationId=31400000-8ce0-11bd-b23e-10b96e4ef00f][clientSubscriptionId=327d9145-4965-4d28-a2c5-39dedee50334] file transmission notification service request to customs notification failed.")
        .withByNameParam(emulatedServiceFailure)
        .verify()
    }

  }

  private def fakeRequestWith(json: JsValue): FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(json)

}
