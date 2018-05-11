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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.controllers.UpscanNotificationController

class UpscanNotificationControllerSpec extends PlaySpec with MockitoSugar with Results {

  private val decId = UUID.randomUUID().toString
  private val eori = UUID.randomUUID().toString
  private val docType = "license"
  private val clientSubscriptionId = UUID.randomUUID().toString
  private val fileReference = UUID.randomUUID().toString


  val controller = new UpscanNotificationController()
  implicit val ResponseContentsReads = Json.reads[ResponseContents]

  private val validJson = Json.parse(
    s"""
       |{
       |    "reference" : "$fileReference",
       |    "fileStatus" : "READY",
       |    "url" : "https://some-url"
       |}
    """.stripMargin)

  "upscan notification controller" should {

    "return 204 when a valid request is received" in {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest().withJsonBody(validJson))
      status(result) mustBe 204
      contentAsString(result) mustBe ""
    }

    "return 400 when the request does not contain valid json" in {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest().withTextBody("some").withHeaders((CONTENT_TYPE, "application/json")))
      status(result) mustBe 400
      await(result) mustBe ErrorResponse.errorBadRequest("Invalid JSON payload").JsonResult
    }

    "return 400 when the JSON is not according to the expected schema" in {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest().withJsonBody(Json.parse(
        """
          |{}
        """.stripMargin)))
      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe "Json is not as expected"
    }


    "return 400 when reference is not present" in {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest().withJsonBody(Json.parse(
        """{
          | "fileStatus" : "READY",
          | "url" : "https://some-url"}
        """.stripMargin)))
      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe "Json is not as expected"
    }

    "return 400 when reference  is not valid UUID" in {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest().withJsonBody(Json.parse(
        s"""{
            | "reference" : "123",
            | "fileStatus" : "READY",
            | "url" : "https://some-url"}
        """.stripMargin)))

      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe "Json is not as expected"
    }

    "return 400 when fileStatus is missing" in {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest().withJsonBody(Json.parse(
        s"""{
            | "reference" : "$fileReference",
            | "url" : "https://some-url"
            }""".stripMargin)))

      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe "Json is not as expected"
    }

    "return 400 when fileStatus is not READY or FAILED" in {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest().withJsonBody(Json.parse(
        s"""{
            | "reference" : "$fileReference",
            | "fileStatus" : "fdf",
            | "url" : "https://some-url"}
        """.stripMargin)))

      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe "Json is not as expected"
    }

    "return 400 when fileStatus is FAILED and Details is not available" in {
      val result = controller.post(decId, eori, docType, clientSubscriptionId).apply(FakeRequest().withJsonBody(Json.parse(
        s"""{
            | "reference" : "$fileReference",
            | "fileStatus" : "FAILED",
            | "url" : "https://some-url"
            | }
        """.stripMargin)))

      status(result) mustBe 400
      val badRequestJsValue: ResponseContents = contentAsJson(result).as[ResponseContents]
      badRequestJsValue.code mustBe "Json is not as expected"
      //      badRequestJsValue.message mustBe "Json is not as expected"
    }


  }
}
