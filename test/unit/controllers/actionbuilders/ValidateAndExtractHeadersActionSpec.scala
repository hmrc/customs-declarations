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

package unit.controllers.actionbuilders

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{HeaderValidator, ValidateAndExtractHeadersAction}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{ConversationIdRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.play.test.UnitSpec
import util.RequestHeaders
import util.TestData._

class ValidateAndExtractHeadersActionSpec extends UnitSpec with MockitoSugar with TableDrivenPropertyChecks {

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockHeaderValidator: HeaderValidator = mock[HeaderValidator]
    val validateAndExtractHeadersAction: ValidateAndExtractHeadersAction = new ValidateAndExtractHeadersAction(mockHeaderValidator, mockLogger)
  }

  val headersTable =
    Table(
      ("description", "result of validation", "expected response"),
      ("Valid Headers", Right(TestExtractedHeaders), Right(TestValidatedHeadersRequest)),
      ("Invalid header", Left(ErrorContentTypeHeaderInvalid),
        Left(ErrorContentTypeHeaderInvalid.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationIdValue)))
    )

  "HeaderValidatorAction" should  {
    forAll(headersTable) { (description, validationResult, expectedResult) =>
      s"$description" in new SetUp() {
        val conversationIdRequest = TestConversationIdRequest
        when(mockHeaderValidator.validateHeaders(any[ConversationIdRequest[_]])).thenReturn(validationResult)

        val actualResult: Either[Result, ValidatedHeadersRequest[_]] = await(validateAndExtractHeadersAction.refine(conversationIdRequest))

        actualResult shouldBe expectedResult
      }
    }
  }
}
