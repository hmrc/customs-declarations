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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.controllers.{FileStatus, UpscanNotification, UpscanNotificationController}
import uk.gov.hmrc.customs.declaration.services.{UploadedFileDetails, UploadedFileProcessingService}

import scala.concurrent.Future

class UpscanNotificationControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfterEach {

  private val fileReference = UUID.randomUUID()
  private val mockService = mock[UploadedFileProcessingService]


  private val post: Action[AnyContent] = new UpscanNotificationController(mockService)
    .post("decId", "eori", "docType", "clientSubscriptionId")

  override def beforeEach()  {
    reset(mockService)
  }


  "upscan notification controller" should {

    "return 204 when a valid READY request is received" in {
      val result = post (fakeRequestWith(readyPayload))

      status(result) mustBe 204
      contentAsString(result) mustBe ""
      verify(mockService).sendMessage(UploadedFileDetails("decId", "eori", "docType", "clientSubscriptionId", UpscanNotification(fileReference, FileStatus.READY, None, Some("https://some-url"))))
    }

    "return 204 even when call to Custom Notifications services fails" in {
      when(mockService.sendMessage(any[UploadedFileDetails]))
        .thenReturn(Future.failed(new RuntimeException("something")))

      val result = post (fakeRequestWith(readyPayload))

      status(result) mustBe 204
      contentAsString(result) mustBe ""
      verify(mockService).sendMessage(UploadedFileDetails("decId", "eori", "docType", "clientSubscriptionId", UpscanNotification(fileReference, FileStatus.READY, None, Some("https://some-url"))))
    }

    "return 204 when a valid FAILED request is received" in {
      val result = post (fakeRequestWith(failedFileStatusPayload))

      status(result) mustBe 204
      contentAsString(result) mustBe ""
    }

    "return 400 when the request does not contain valid json" in {
      val result = post (FakeRequest().withTextBody("some").withHeaders((CONTENT_TYPE, "application/json")))

      status(result) mustBe 400
      await(result) mustBe ErrorResponse.errorBadRequest("Invalid JSON payload").JsonResult
    }

    "return 400 when request does not contain the reference" in {
      val result = post (fakeRequestWith(readyPayload - "reference"))

      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe expectedJsonValidationMessage
      badRequestJsValue.message must include("/reference,List(ValidationError(List(error.path.missing")
    }

    "return 400 when reference  is not valid UUID" in {
      val result = post (fakeRequestWith(readyPayload + ("reference" -> JsString("123"))))

      status(result) mustBe 400
      val badRequestJsValue = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe expectedJsonValidationMessage
      badRequestJsValue.message must include("/reference,List(ValidationError(List(error.expected.uuid")
    }

    "return 400 when fileStatus is missing" in {
      val result = post (fakeRequestWith(readyPayload - "fileStatus"))

      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe expectedJsonValidationMessage
      badRequestJsValue.message must include("/fileStatus,List(ValidationError(List(error.path.missing")
    }

    "return 400 when fileStatus is not READY or FAILED" in {
      val result = post (fakeRequestWith(readyPayload +("fileStatus" -> JsString("123"))))

      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe expectedJsonValidationMessage
      badRequestJsValue.message must include("/fileStatus,List(ValidationError(List(error.expected.validenumvalue")
    }

    "return 400 when fileStatus is FAILED and details is not available" in {
      val result = post (fakeRequestWith(failedFileStatusPayload - "details"))

      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe expectedJsonValidationMessage
      badRequestJsValue.message must include("File status is FAILED so details are required")
    }
  }

  implicit val ResponseContentsReads = Json.reads[ResponseContents]

  private val readyPayload = Json.parse(
    s"""
       |{
       |    "reference" : "$fileReference",
       |    "fileStatus" : "READY",
       |    "url" : "https://some-url"
       |}
    """.stripMargin).as[JsObject]

  private val failedFileStatusPayload = Json.parse(
    s"""
       |{
       |    "reference" : "$fileReference",
       |    "fileStatus" : "FAILED",
       |    "details": "some failure details"
       |}
    """.stripMargin).as[JsObject]

  private val expectedJsonValidationMessage = "Unexpected JSON"

  private def fakeRequestWith(json: JsValue) =
    FakeRequest().withJsonBody(json)

}
