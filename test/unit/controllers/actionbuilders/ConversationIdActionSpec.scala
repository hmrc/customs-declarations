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

import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.ConversationIdAction
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.customs.declaration.services.DateTimeService
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.TestData.{conversationId, stubUniqueIdsService}

import scala.concurrent.ExecutionContext

class ConversationIdActionSpec extends AnyWordSpecLike with MockitoSugar with Matchers {

  trait SetUp {

    protected implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
    private val mockExportsLogger = mock[DeclarationsLogger]
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]

    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val conversationIdAction = new ConversationIdAction(mockExportsLogger, stubUniqueIdsService, mockDateTimeService)
    val expected: ConversationIdRequest[AnyContentAsEmpty.type] = ConversationIdRequest(conversationId, EventStart, request)
  }

  "ConversationIdAction" should {
    "Generate a request containing a unique conversation id" in new SetUp {
      when(mockDateTimeService.zonedDateTimeUtc).thenReturn(EventStart)

      private val actual = (conversationIdAction.transform(request).futureValue)

      actual shouldBe expected
    }
  }

}
