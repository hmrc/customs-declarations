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

import org.mockito.ArgumentMatchers.{eq => meq}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.controllers._
import uk.gov.hmrc.customs.declaration.model.FileTransmissionNotification
import uk.gov.hmrc.customs.declaration.services.{BatchFileUploadNotificationService, FileTransmissionCallbackToXmlNotification}
import util.ApiSubscriptionFieldsTestData.{subscriptionFieldsId, subscriptionFieldsIdString}
import util.FileTransmissionTestData._
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData

import scala.concurrent.Future

class FileTransmissionNotificationControllerSpec extends PlaySpec
  with MockitoSugar {

  val mockCdsLogger = mock[CdsLogger]

  trait SetUp {

    val mockService = mock[BatchFileUploadNotificationService]
    implicit val callbackToXmlNotification = mock[FileTransmissionCallbackToXmlNotification]

    val controller = new FileTransmissionNotificationController(callbackToXmlNotification, mockService, mockCdsLogger)
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
      verifyZeroInteractions(mockService)
    }

    "return 400 when a invalid json is received" in new SetUp {
      val result = controller.post(subscriptionFieldsIdString)(FakeRequest().withTextBody("some").withHeaders((CONTENT_TYPE, "application/json")))

      status(result) mustBe BAD_REQUEST

      contentAsString(result) mustBe BadRequestErrorResponseInvalidJson
      verifyZeroInteractions(mockService)
      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam[String]("Malformed JSON received. Body: Some(some) headers: List((Content-Type,application/json))")
        .verify()
    }

    "return 500 when call to Custom Notification services fails" in new SetUp {
      when(mockService.sendMessage[FileTransmissionNotification](SuccessNotification, SuccessNotification.fileReference, subscriptionFieldsId)(callbackToXmlNotification)).thenReturn(Future.failed(TestData.emulatedServiceFailure))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionSuccessNotificationPayload)))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe InternalErrorResponseJson
      verify(mockService).sendMessage[FileTransmissionNotification](SuccessNotification, SuccessNotification.fileReference, subscriptionFieldsId)(callbackToXmlNotification)
    }

  }

  private def fakeRequestWith(json: JsValue): FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(json)

}
