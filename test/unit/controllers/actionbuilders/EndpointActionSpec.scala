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

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.EndpointAction
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsValues
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsValues.Submit
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AnalyticsValuesAndConversationIdRequest
import uk.gov.hmrc.customs.declaration.services.{DateTimeService, UniqueIdsService}
import uk.gov.hmrc.play.test.UnitSpec
import util.CustomsDeclarationsMetricsTestData.EventStart
import util.TestData
import util.TestData.conversationId

class EndpointActionSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    private val mockExportsLogger = mock[DeclarationsLogger]
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]

    val request = FakeRequest()
    val endpointAction = new EndpointAction {
      override val logger: DeclarationsLogger = mockExportsLogger
      override val googleAnalyticsValues: GoogleAnalyticsValues = Submit
      override val correlationIdService: UniqueIdsService = TestData.stubUniqueIdsService
      override val timeService: DateTimeService = mockDateTimeService
    }
    val expected = AnalyticsValuesAndConversationIdRequest(conversationId, Submit, EventStart, request)
  }

  "EndpointAction" should {
    "Generate a Request containing a unique correlation id" in new SetUp {
      when(mockDateTimeService.zonedDateTimeUtc).thenReturn(EventStart)

      private val actual = await(endpointAction.transform(request))

      actual shouldBe expected
    }
  }

}
