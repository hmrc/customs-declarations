package unit.controllers

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.controllers._
import uk.gov.hmrc.customs.declaration.services.FileTransmissionNotificationService
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsIdString
import util.FileTransmissionTestData.{FileTransmissionSuccessNotificationPayload, FileTransmissionFailureNotificationPayload, InternalErrorResponseJson}
import util.TestData._

import scala.concurrent.Future

class FileTransmissionNotificationControllerSpec extends PlaySpec
  with MockitoSugar {

  trait SetUp {

    val mockCdsLogger = mock[CdsLogger]
    val mockService = mock[FileTransmissionNotificationService]

    val controller = new FileTransmissionNotificationController(mockService, mockCdsLogger)
    val successNotification = FileTransmissionNotification(FileReferenceOne, BatchIdOne, FileTransmissionStatus.SUCCESS, None)
    val failureNotification = successNotification.copy(errorDetails = Some("some error text"))

  }

  "file transmission notification controller" should {

    "return 204 when a valid SUCCESS request is received" in new SetUp {
      when(mockService.sendMessage(successNotification, subscriptionFieldsIdString)).thenReturn(Future.successful(Right(())))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(FileTransmissionSuccessNotificationPayload))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      verify(mockService).sendMessage(successNotification, subscriptionFieldsIdString)
    }

    "return 204 when a valid FAILURE request is received" in new SetUp {
      when(mockService.sendMessage(failureNotification, subscriptionFieldsIdString)).thenReturn(Future.successful(Right(())))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(FileTransmissionFailureNotificationPayload))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      verify(mockService).sendMessage(failureNotification, subscriptionFieldsIdString)
    }

    "return 500 even when call to Custom Notifications services fails" in new SetUp {
      when(mockService.sendMessage(successNotification, subscriptionFieldsIdString)).thenReturn(Future.successful(Left(ErrorResponse.ErrorInternalServerError.JsonResult)))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(FileTransmissionSuccessNotificationPayload))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe InternalErrorResponseJson
      verify(mockService).sendMessage(successNotification, subscriptionFieldsIdString)
    }

  }

  private def fakeRequestWith(json: JsValue): FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(json)
  }
