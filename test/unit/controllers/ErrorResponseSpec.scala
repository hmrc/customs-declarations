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

package unit.controllers

import org.apache.pekko.stream.Materializer
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.mvc.Http.Status._
import util.UnitSpec
import uk.gov.hmrc.customs.declaration.controllers.{ErrorResponse, HttpStatusCodeShortDescriptions, ResponseContents}
import scala.xml.Utility.trim
import scala.xml.XML.loadString

class ErrorResponseSpec extends HttpStatusCodeShortDescriptions with UnitSpec with MockitoSugar {

  private implicit val mockMaterializer: Materializer = mock[Materializer]

  private val someStatusCode = 123

  private val errCode = "123_code"
  private val errorMessage: String = "123 code message"

  private val errorResponseWithoutExtraErrors = ErrorResponse(someStatusCode, errCode, errorMessage)
  val errorResponseWithExtraErrors = ErrorResponse(someStatusCode, errCode, errorMessage,
    ResponseContents("first validation failure", "problem at some field /1"),
    ResponseContents("second validation failure", "problem at some field /101")
  )
  val errorResponseWithExtraErrorsUsingBuilder = ErrorResponse(someStatusCode, errCode, errorMessage).withErrors(
    ResponseContents("first validation failure", "problem at some field /1"),
    ResponseContents("second validation failure", "problem at some field /101")
  )

  "Json Result" should {

    "return correct http status code" in {
      status(errorResponseWithoutExtraErrors.JsonResult) shouldBe someStatusCode
    }

    "content without errors" in {
      val actual = errorResponseWithoutExtraErrors.JsonResult
      jsonBodyOf(actual)(mockMaterializer) shouldBe Json.parse(
        s"""
          |{
          |   "code":"$errCode",
          |   "message":"$errorMessage"
          |}
        """.stripMargin)
    }

    "contents with extra error details" in {
      val actual = errorResponseWithExtraErrors.JsonResult

      jsonBodyOf(actual)(mockMaterializer) shouldBe Json.parse(
        s"""
          |{
          |   "code":"$errCode",
          |   "message":"$errorMessage",
          |   "errors": [
          |     {
          |       "code":"first validation failure",
          |       "message":"problem at some field /1"
          |     },
          |     {
          |       "code":"second validation failure",
          |       "message":"problem at some field /101"
          |     }
          |   ]
          |}
        """.stripMargin)
    }

    "contents with extra error details using builder" in {
      val actual = errorResponseWithExtraErrorsUsingBuilder.JsonResult

      jsonBodyOf(actual)(mockMaterializer) shouldBe Json.parse(
        s"""
           |{
           |   "code":"$errCode",
           |   "message":"$errorMessage",
           |   "errors": [
           |     {
           |       "code":"first validation failure",
           |       "message":"problem at some field /1"
           |     },
           |     {
           |       "code":"second validation failure",
           |       "message":"problem at some field /101"
           |     }
           |   ]
           |}
        """.stripMargin)
    }
  }

  "XML Result" should {

    "return correct http status code" in {
      status(errorResponseWithoutExtraErrors.XmlResult) shouldBe someStatusCode
    }

    "content without errors" in {
      val actual = errorResponseWithoutExtraErrors.XmlResult
      trim(loadString(bodyOf(actual))) shouldBe trim(
        <errorResponse>
          <code>123_code</code>
          <message>123 code message</message>
        </errorResponse>)
    }

    "contents with extra error details" in {
      val actual = errorResponseWithExtraErrors.XmlResult

      trim(loadString(bodyOf(actual))) shouldBe trim(
        <errorResponse>
          <code>123_code</code>
          <message>123 code message</message>
          <errors>
            <error>
              <code>first validation failure</code>
              <message>problem at some field /1</message>
            </error>
            <error>
              <code>second validation failure</code>
              <message>problem at some field /101</message>
            </error>
          </errors>
        </errorResponse>)
    }

    "contents with extra error details using builder" in {
      val actual = errorResponseWithExtraErrorsUsingBuilder.XmlResult

      trim(loadString(bodyOf(actual))) shouldBe trim(
        <errorResponse>
          <code>123_code</code>
          <message>123 code message</message>
          <errors>
            <error>
              <code>first validation failure</code>
              <message>problem at some field /1</message>
            </error>
            <error>
              <code>second validation failure</code>
              <message>problem at some field /101</message>
            </error>
          </errors>
        </errorResponse>)
    }
  }

  "ErrorResponse" should {

    "create correct Bad Request" in {
      ErrorResponse.errorBadRequest("some error message", "someErrorCode") shouldBe
        ErrorResponse(BAD_REQUEST, "someErrorCode", "some error message")
    }

    "create correct Internal server error" in {
      ErrorResponse.errorInternalServerError("some internal error message") shouldBe
        ErrorResponse(INTERNAL_SERVER_ERROR, InternalServerErrorCode, "some internal error message")
    }

    "create correct payload forbidden error" in {
      ErrorResponse.ErrorPayloadForbidden shouldBe
        ErrorResponse(FORBIDDEN, PayloadForbidden, "A firewall rejected the request")
    }
  }

}
