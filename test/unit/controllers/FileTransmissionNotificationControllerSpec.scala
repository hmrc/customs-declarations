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
import uk.gov.hmrc.customs.declaration.services.{BatchFileNotificationService, FileTransmissionCallbackToXmlNotification}
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsIdString
import util.FileTransmissionTestData._

import scala.concurrent.Future

class FileTransmissionNotificationControllerSpec extends PlaySpec
  with MockitoSugar {

  trait SetUp {

    val mockCdsLogger = mock[CdsLogger]
    val mockService = mock[BatchFileNotificationService]
    implicit val mockFileTransmissionCallBackToXmlNotification = mock[FileTransmissionCallbackToXmlNotification]

    val controller = new FileTransmissionNotificationController(mockFileTransmissionCallBackToXmlNotification, mockService, mockCdsLogger)
  }

  "file transmission notification controller" should {

    "return 204 when a valid SUCCESS request is received" in new SetUp {
      when(mockService.sendMessage(SuccessNotification, subscriptionFieldsIdString)(mockFileTransmissionCallBackToXmlNotification)).thenReturn(Future.successful(()))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionSuccessNotificationPayload)))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      verify(mockService).sendMessage(SuccessNotification, subscriptionFieldsIdString)
    }

    "return 204 when a valid FAILURE request is received" in new SetUp {
      when(mockService.sendMessage(FailureNotification, subscriptionFieldsIdString)).thenReturn(Future.successful(()))

      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(Json.parse(FileTransmissionFailureNotificationPayload)))

      status(result) mustBe NO_CONTENT
      contentAsString(result) mustBe empty
      verify(mockService).sendMessage(FailureNotification, subscriptionFieldsIdString)
    }

    //this no longer applies
//    "return 500 even when call to Custom Notifications services fails" in new SetUp {
//      when(mockService.sendMessage(successNotification, subscriptionFieldsIdString)).thenReturn(Future.successful(()))
//
//      val result = controller.post(subscriptionFieldsIdString)(fakeRequestWith(FileTransmissionSuccessNotificationPayload))
//
//      status(result) mustBe INTERNAL_SERVER_ERROR
//      contentAsString(result) mustBe InternalErrorResponseJson
//      verify(mockService).sendMessage(successNotification, subscriptionFieldsIdString)
//    }

  }

  private def fakeRequestWith(json: JsValue): FakeRequest[AnyContentAsJson] =
    FakeRequest().withJsonBody(json)
  }
