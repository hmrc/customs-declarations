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

package unit.config

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment, Mode, OptionalSourceMapper}
import uk.gov.hmrc.customs.declaration.config.CustomsErrorHandler
import util.UnitSpec

import javax.inject.Provider
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.xml.{Node, Utility, XML}

class CustomErrorHandlerSpec extends UnitSpec with MockitoSugar {

  private val HMRCJsonMimeType = "application/vnd.hmrc.1.0+json"
  private val HMRCXmlMimeType = "application/vnd.hmrc.1.0+xml"

  private val AcceptHmrcJsonRequest = FakeRequest().withHeaders(ACCEPT -> HMRCJsonMimeType)
  private val AcceptJsonRequest = FakeRequest().withHeaders(ACCEPT -> "application/json")
  private val AcceptHmrcXmlRequest = FakeRequest().withHeaders(ACCEPT -> HMRCXmlMimeType)
  private val AcceptXmlRequest = FakeRequest().withHeaders(ACCEPT -> "application/xml")
  private val AcceptAllRequest = FakeRequest().withHeaders(ACCEPT -> "*")

  private val ErrorMessage = "Some error message."

  private val ServerException = new Exception(ErrorMessage)

  private def errorXml(errorCode: String, errorMessage: String = ErrorMessage) = string2xml(
    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<errorResponse>
       |  <code>$errorCode</code>
       |  <message>$errorMessage</message>
       |</errorResponse>
    """.stripMargin)

  private def errorJson(errorCode: String, errorMessage: String = ErrorMessage) = Json.parse(
    s"""
       |{
       |     "code": "$errorCode",
       |     "message": "$errorMessage"
       |}
     """.stripMargin)

  private class CustomErrorHandlerFixture {
    val mockEnvironment = mock[Environment]
    val mockConfiguration = mock[Configuration]
    val emptySourceMapper = new OptionalSourceMapper(None)
    val mockRouter = mock[Provider[Router]]

    lazy val CustomErrorHandler = new CustomsErrorHandler(mockEnvironment, mockConfiguration, emptySourceMapper, mockRouter)
  }

  private def withFixture(test: CustomErrorHandlerFixture => Unit) = test(new CustomErrorHandlerFixture)

  "CustomErrorHandler.onBadRequest" should {

    s"produce BAD_REQUEST JSON Result when caller accepts ${AcceptHmrcJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptHmrcJsonRequest)
    }

    s"produce BAD_REQUEST JSON Result when caller accepts ${AcceptJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptJsonRequest)
    }

    s"produce BAD_REQUEST JSON Result when caller accepts ${AcceptAllRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptAllRequest)
    }

    s"produce BAD_REQUEST XML Result when caller accepts ${AcceptHmrcXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptHmrcXmlRequest)
    }

    s"produce BAD_REQUEST XML Result when caller accepts ${AcceptXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptXmlRequest)
    }

    def assertJsonResult(acceptJsonRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      val result = onClientError(acceptJsonRequest, BAD_REQUEST, ErrorMessage)
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe errorJson("BAD_REQUEST")
    }

    def assertXmlResult(acceptXmlRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      val result = onClientError(acceptXmlRequest, BAD_REQUEST, ErrorMessage)
      status(result) shouldBe BAD_REQUEST
      contentAsXml(result) shouldBe errorXml("BAD_REQUEST")
    }
  }

  "CustomErrorHandler.onNotFound" should {

    s"produce NOT_FOUND JSON Result when caller accepts ${AcceptHmrcJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptHmrcJsonRequest)
    }

    s"produce NOT_FOUND JSON Result when caller accepts ${AcceptJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptJsonRequest)
    }

    s"produce NOT_FOUND JSON Result when caller accepts ${AcceptAllRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptAllRequest)
    }

    s"produce NOT_FOUND XML Result when caller accepts ${AcceptHmrcXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptHmrcXmlRequest)
    }

    s"produce NOT_FOUND XML Result when caller accepts ${AcceptXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptXmlRequest)
    }

    def assertJsonResult(acceptJsonRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      val result = onClientError(acceptJsonRequest, NOT_FOUND, ErrorMessage)
      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe errorJson("NOT_FOUND", "Resource was not found")
    }

    def assertXmlResult(acceptXmlRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      val result = onClientError(acceptXmlRequest, NOT_FOUND, ErrorMessage)
      status(result) shouldBe NOT_FOUND
      contentAsXml(result) shouldBe errorXml("NOT_FOUND", "Resource was not found")
    }
  }

  "CustomErrorHandler.onOtherClientError" should {

    s"produce BAD_REQUEST JSON Result when caller accepts ${AcceptHmrcJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptHmrcJsonRequest)
    }

    s"produce BAD_REQUEST JSON Result when caller accepts ${AcceptJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptJsonRequest)
    }

    s"produce BAD_REQUEST JSON Result when caller accepts ${AcceptAllRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptAllRequest)
    }

    s"produce BAD_REQUEST XML Result when caller accepts ${AcceptHmrcXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptHmrcXmlRequest)
    }

    s"produce BAD_REQUEST XML Result when caller accepts ${AcceptXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptXmlRequest)
    }

    def assertJsonResult(acceptJsonRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      val result = onClientError(acceptJsonRequest, EXPECTATION_FAILED, ErrorMessage)
      status(result) shouldBe EXPECTATION_FAILED
      contentAsJson(result) shouldBe errorJson("BAD_REQUEST")
    }

    def assertXmlResult(acceptXmlRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      val result = onClientError(acceptXmlRequest, EXPECTATION_FAILED, ErrorMessage)
      status(result) shouldBe EXPECTATION_FAILED
      contentAsXml(result) shouldBe errorXml("BAD_REQUEST")
    }
  }

  "CustomErrorHandler.onDevServerError" should {

    s"produce 500 JSON Result with exception message when caller accepts ${AcceptHmrcJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptHmrcJsonRequest)
    }

    s"produce 500 JSON Result with exception message when caller accepts ${AcceptJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptJsonRequest)
    }

    s"produce 500 JSON Result with exception message when caller accepts ${AcceptAllRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptAllRequest)
    }

    s"produce 500 XML Result with exception message when caller accepts ${AcceptHmrcXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptHmrcXmlRequest)
    }

    s"produce 500 XML Result with exception message when caller accepts ${AcceptXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptXmlRequest)
    }

    def assertJsonResult(acceptJsonRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      when(fixture.mockEnvironment.mode) thenReturn Mode.Dev

      val result = onServerError(acceptJsonRequest, ServerException)
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe errorJson("INTERNAL_SERVER_ERROR", s"Execution exception[[${ServerException.getClass.getSimpleName}: $ErrorMessage]]")
    }

    def assertXmlResult(acceptXmlRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      when(fixture.mockEnvironment.mode) thenReturn Mode.Dev

      val result = onServerError(acceptXmlRequest, ServerException)
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsXml(result) shouldBe errorXml("INTERNAL_SERVER_ERROR", s"Execution exception[[${ServerException.getClass.getSimpleName}: $ErrorMessage]]")
    }
  }

  "CustomErrorHandler.onProdServerError" should {

    s"produce INTERNAL_SERVER_ERROR JSON Result when caller accepts ${AcceptHmrcJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptHmrcJsonRequest)
    }

    s"produce INTERNAL_SERVER_ERROR JSON Result when caller accepts ${AcceptJsonRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptJsonRequest)
    }

    s"produce INTERNAL_SERVER_ERROR JSON Result when caller accepts ${AcceptAllRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertJsonResult(AcceptAllRequest)
    }

    s"produce INTERNAL_SERVER_ERROR XML Result when caller accepts ${AcceptHmrcXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptHmrcXmlRequest)
    }

    s"produce INTERNAL_SERVER_ERROR XML Result when caller accepts ${AcceptXmlRequest.headers(ACCEPT)}" in withFixture { implicit fixture =>
      assertXmlResult(AcceptXmlRequest)
    }

    def assertJsonResult(acceptJsonRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      when(fixture.mockEnvironment.mode) thenReturn Mode.Prod

      val result = onServerError(acceptJsonRequest, ServerException)
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe errorJson("INTERNAL_SERVER_ERROR", "Internal server error")
    }

    def assertXmlResult(acceptXmlRequest: RequestHeader)(implicit fixture: CustomErrorHandlerFixture): Unit = {
      when(fixture.mockEnvironment.mode) thenReturn Mode.Prod

      val result = onServerError(acceptXmlRequest, ServerException)
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsXml(result) shouldBe errorXml("INTERNAL_SERVER_ERROR", "Internal server error")
    }
  }

  private def contentAsXml(of: Future[Result]): Node = string2xml(contentAsString(of))

  private def string2xml(s: String): Node = {
    val xml = try {
      XML.loadString(s)
    } catch {
      case NonFatal(thr) => fail("Not an xml: " + s, thr)
    }
    Utility.trim(xml)
  }

  private def onClientError(request: RequestHeader,
                            statusCode: Int,
                            message: String)(implicit fixture: CustomErrorHandlerFixture): Future[Result] = {
    fixture.CustomErrorHandler.onClientError(request, statusCode, message)
  }

  private def onServerError(request: RequestHeader, thr: Throwable)(implicit fixture: CustomErrorHandlerFixture): Future[Result] = {
    fixture.CustomErrorHandler.onServerError(request, thr)
  }

}
