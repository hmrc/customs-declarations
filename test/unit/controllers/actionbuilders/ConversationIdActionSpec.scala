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

import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.ConversationIdAction
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ConversationIdRequest
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData
import util.TestData.conversationId

class ConversationIdActionSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    private val mockExportsLogger = mock[DeclarationsLogger]
    val request = FakeRequest()
    val conversationIdAction = new ConversationIdAction(TestData.stubUniqueIdsService, mockExportsLogger)
    val expected = ConversationIdRequest(conversationId, request)
  }

  "ConversationIdAction" should {
    "transform Request to CorrelationIdsRequest" in new SetUp {

      private val actual = await(conversationIdAction.transform(request))

      actual shouldBe expected
    }
  }

}
