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

package unit.controllers.actionbuilders

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.Helpers
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{HeaderStatusValidator, ValidateAndExtractHeadersStatusAction}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ApiVersionRequest, ValidatedHeadersStatusRequest}
import util.RequestHeaders
import util.TestData._

import scala.concurrent.ExecutionContext

class ValidateAndExtractHeadersStatusActionSpec extends AnyWordSpecLike with MockitoSugar with TableDrivenPropertyChecks with Matchers {

  trait SetUp {
    implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockHeaderStatusValidator: HeaderStatusValidator = mock[HeaderStatusValidator]
    val validateAndExtractHeadersAction: ValidateAndExtractHeadersStatusAction = new ValidateAndExtractHeadersStatusAction(mockHeaderStatusValidator, mockLogger)
  }

  "HeaderValidationStatusAction when validation succeeds" should {
    "extract headers from incoming request and copy relevant values on to the ValidatedHeaderStatusRequest" in new SetUp {
      val apiVersionRequestV1: ApiVersionRequest[AnyContentAsXml] = TestApiVersionRequestV1
      when(mockHeaderStatusValidator.validateHeaders(any[ApiVersionRequest[Any]])).thenReturn(Right(TestExtractedStatusHeaders))

      val actualResult: Either[Result, ValidatedHeadersStatusRequest[_]] = validateAndExtractHeadersAction.refine(apiVersionRequestV1).futureValue

      actualResult shouldBe Right(TestValidatedHeadersStatusRequest)
    }
  }

  "HeaderValidationStatusAction when validation fails" should {
    "return error with conversation Id in the headers" in new SetUp {
      val apiVersionRequestV1: ApiVersionRequest[AnyContentAsXml] = TestApiVersionRequestV1
      when(mockHeaderStatusValidator.validateHeaders(any[ApiVersionRequest[Any]])).thenReturn(Left(ErrorContentTypeHeaderInvalid))

      val actualResult: Either[Result, ValidatedHeadersStatusRequest[_]] = validateAndExtractHeadersAction.refine(apiVersionRequestV1).futureValue

      actualResult shouldBe Left(ErrorContentTypeHeaderInvalid.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationIdValue))
    }
  }
}
