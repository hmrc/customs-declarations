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

package unit.services

import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.StatusResponse
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, StatusResponseFilterService}
import uk.gov.hmrc.customs.declaration.xml.StatusResponseCreator
import uk.gov.hmrc.play.test.UnitSpec
import util.TestXMLData

import scala.xml.NodeSeq

class StatusResponseFilterServiceSpec extends UnitSpec with MockitoSugar {

  trait SetUp {

    val mockStatusResponseCreator = mock[StatusResponseCreator]
    val mockDeclarationsLogger = mock[DeclarationsLogger]
    val mockDeclarationsConfigService = mock[DeclarationsConfigService]

    val service = new StatusResponseFilterService(mockStatusResponseCreator, mockDeclarationsLogger, mockDeclarationsConfigService)
    when(mockStatusResponseCreator.create(any[StatusResponse])).thenReturn(any[NodeSeq])
  }

  "StatusResponseFilterService" should {
    "return filtered values" in new SetUp() {
      val statusResponseCaptor: ArgumentCaptor[StatusResponse] = ArgumentCaptor.forClass(classOf[StatusResponse])

      service.filter(TestXMLData.generateValidDEC65Response(DateTime.now().toString))

      verify(mockStatusResponseCreator).create(statusResponseCaptor.capture())
      val statusResponse = statusResponseCaptor.getValue

      statusResponse.versionNumber.get shouldBe "0"
      statusResponse.creationDate.get shouldBe "2001-12-17T09:30:47Z"
      statusResponse.goodsItemCount.get shouldBe "2"
      statusResponse.tradeMovementType.get shouldBe "trade movement type"
      statusResponse.declarationType.get shouldBe "declaration type"
      statusResponse.packageCount.get shouldBe "3"
      statusResponse.acceptanceDate.get shouldBe "2002-12-17T09:30:47Z"
      statusResponse.partyIdentificationNumbers.get shouldBe List(Some("1"))
    }

    "return filtered values when only two parties with id numbers and one without present" in new SetUp() {
      val statusResponseCaptor: ArgumentCaptor[StatusResponse] = ArgumentCaptor.forClass(classOf[StatusResponse])

      service.filter(TestXMLData.generateValidStatusResponseWithMultiplePartiesOnly)

      verify(mockStatusResponseCreator).create(statusResponseCaptor.capture())
      val statusResponse = statusResponseCaptor.getValue

      statusResponse.versionNumber shouldBe None
      statusResponse.creationDate shouldBe None
      statusResponse.goodsItemCount shouldBe None
      statusResponse.tradeMovementType shouldBe None
      statusResponse.declarationType shouldBe None
      statusResponse.packageCount shouldBe None
      statusResponse.acceptanceDate shouldBe None
      statusResponse.partyIdentificationNumbers.get shouldBe List(Some("1"), Some("2"), None)
    }

    "return no filtered values" in new SetUp() {
      val statusResponseCaptor: ArgumentCaptor[StatusResponse] = ArgumentCaptor.forClass(classOf[StatusResponse])

      service.filter(TestXMLData.generateValidStatusResponseNoStatusValues)

      verify(mockStatusResponseCreator).create(statusResponseCaptor.capture())
      val statusResponse = statusResponseCaptor.getValue

      statusResponse.versionNumber shouldBe None
      statusResponse.creationDate shouldBe None
      statusResponse.goodsItemCount shouldBe None
      statusResponse.tradeMovementType shouldBe None
      statusResponse.declarationType shouldBe None
      statusResponse.packageCount shouldBe None
      statusResponse.acceptanceDate shouldBe None
      statusResponse.partyIdentificationNumbers shouldBe None
    }
  }

}
