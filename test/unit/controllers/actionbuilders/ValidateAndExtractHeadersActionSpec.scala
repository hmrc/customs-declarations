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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{HeaderWithContentTypeValidator, ValidateAndExtractHeadersAction}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ApiVersionRequest, ValidatedHeadersRequest}
import util.TestData._
import util.RequestHeaders

import scala.concurrent.ExecutionContext

class ValidateAndExtractHeadersActionSpec extends AnyWordSpecLike with MockitoSugar with TableDrivenPropertyChecks with Matchers {

  trait SetUp {
    implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockHeaderValidator: HeaderWithContentTypeValidator = mock[HeaderWithContentTypeValidator]
    val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(mockHeaderValidator)
  }

  "HeaderValidationAction when validation succeeds" should {
    "extract headers from incoming request and copy relevant values on to the ValidatedHeaderRequest" in new SetUp {
      val apiVersionRequestV1: ApiVersionRequest[AnyContentAsXml] = TestApiVersionRequestV1
      when(mockHeaderValidator.validateHeaders(any[ApiVersionRequest[_]])).thenReturn(Right(TestExtractedHeaders))

      val actualResult: Either[Result, ValidatedHeadersRequest[_]] = await(validateAndExtractHeadersAction.refine(apiVersionRequestV1))

      actualResult shouldBe Right(TestValidatedHeadersRequest)
    }
  }

  "HeaderValidationAction when validation fails" should {
    "return error with conversation Id in the headers" in new SetUp {
      val apiVersionRequestV1: ApiVersionRequest[AnyContentAsXml] = TestApiVersionRequestV1
      when(mockHeaderValidator.validateHeaders(any[ApiVersionRequest[_]])).thenReturn(Left(ErrorContentTypeHeaderInvalid))

      val actualResult: Either[Result, ValidatedHeadersRequest[_]] = await(validateAndExtractHeadersAction.refine(apiVersionRequestV1))

      actualResult shouldBe Left(ErrorContentTypeHeaderInvalid.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationIdValue))
    }
  }
}
