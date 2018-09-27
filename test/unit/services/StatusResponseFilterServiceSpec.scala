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

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, StatusResponseFilterService}
import uk.gov.hmrc.play.test.UnitSpec
import util.StatusTestXMLData.{ImportTradeMovementType, generateDeclarationManagementInformationResponse, generateValidStatusResponseWithMultiplePartiesOnly}

import scala.xml.NodeSeq

class StatusResponseFilterServiceSpec extends UnitSpec with MockitoSugar {

  val acceptanceDateVal = DateTime.now(DateTimeZone.UTC)

  trait SetUp {

    val mockDeclarationsLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]

    val service = new StatusResponseFilterService(mockDeclarationsLogger, mockDeclarationsConfigService)

    def createStatusResponseWithAllValues(): NodeSeq = service.transform(generateDeclarationManagementInformationResponse(acceptanceDate = acceptanceDateVal))
  }

  "Status Response Filter Service" should {

    "create the version number" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "versionNumber"

      node.text shouldBe "0"
    }

    "create the creation date" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "creationDate"

      node.text shouldBe "2001-12-17T09:30:47Z"
      node.head.attribute("formatCode").get.text shouldBe "string"
    }

    "create the goods item count" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "goodsItemCount"

      node.text shouldBe "2"
    }

    "create the trade movement type" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "tradeMovementType"

      node.text shouldBe ImportTradeMovementType
    }

    "create the declaration type" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "type"

      node.text shouldBe "declaration type"
    }

    "create the package count" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "packageCount"

      node.text shouldBe "3"
    }

    "create the acceptance date" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "acceptanceDate"

    }

    "create the party identification numbers" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "parties" \ "partyIdentification" \ "number"

      node.head.text shouldBe "1"
    }

    "create the party identification numbers when there are two parties with id numbers and one without id" in new SetUp{
      private val response = service.transform(generateValidStatusResponseWithMultiplePartiesOnly)
      private val node = response \\ "parties"

      (node.head \ "partyIdentification" \ "number").head.text shouldBe "1"
      (node(1) \ "partyIdentification" \ "number").head.text shouldBe "2"
      (node(2) \ "partyIdentification" \ "number").size shouldBe 0
    }

    "not create acceptance date when not provided" in new SetUp {
      private val response = service.transform(generateValidStatusResponseWithMultiplePartiesOnly)
      private val node = response \\ "acceptanceDate"

      node shouldBe empty
    }

  }
}
