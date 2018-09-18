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

package unit.xml

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.StatusResponse
import uk.gov.hmrc.customs.declaration.xml.StatusResponseCreator
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.NodeSeq

class StatusResponseCreatorSpec extends UnitSpec with MockitoSugar {

  trait SetUp {

    val mockDeclarationsLogger = mock[DeclarationsLogger]
    val statusResponseCreator = new StatusResponseCreator(mockDeclarationsLogger)

    val statusResponse = StatusResponse(Some("1"), Some("some creation date"), Some("goods item count"),
      Some("trade movement type"), Some("declaration type"), Some("package count"), Some("some acceptance date"),
      Some(Seq(Some("partyId1"), Some("partyId2"))))

    val statusResponseEmptyParties = statusResponse.copy(partyIdentificationNumbers = Some(Seq(None, None)))
    val statusResponseNoParties = statusResponse.copy(partyIdentificationNumbers = None)
    val statusResponseNoAcceptanceDate = statusResponse.copy(acceptanceDate = None)

    def createResponseWithAllValues(): NodeSeq = statusResponseCreator.create(statusResponse)
  }

  "status response creator" should {

    "set the version number" in new SetUp {
      private val response = createResponseWithAllValues()
      private val node = response \\ "versionNumber"

      node.text shouldBe "1"
    }

    "set the creation date" in new SetUp {
      private val response = createResponseWithAllValues()
      private val node = response \\ "creationDate"

      node.text shouldBe "some creation date"
    }

    "set the goods item count" in new SetUp {
      private val response = createResponseWithAllValues()
      private val node = response \\ "creationDate"

      node.text shouldBe "some creation date"
    }

    "set the trade movement type" in new SetUp {
      private val response = createResponseWithAllValues()
      private val node = response \\ "tradeMovementType"

      node.text shouldBe "trade movement type"
    }

    "set the declaration type" in new SetUp {
      private val response = createResponseWithAllValues()
      private val node = response \\ "type"

      node.text shouldBe "declaration type"
    }

    "set the package count" in new SetUp {
      private val response = createResponseWithAllValues()
      private val node = response \\ "packageCount"

      node.text shouldBe "package count"
    }

    "set the acceptance date" in new SetUp {
      private val response = createResponseWithAllValues()
      private val node = response \\ "acceptanceDate"

      node.text shouldBe "some acceptance date"
    }

    "no acceptance date element when not provided" in new SetUp {
      private val response = statusResponseCreator.create(statusResponseNoAcceptanceDate)
      private val node = response \\ "acceptanceDate"

      node shouldBe empty
    }

    "set the party identification numbers" in new SetUp {
      private val response = createResponseWithAllValues()
      private val node = response \\ "parties" \ "partyIdentification" \ "number"

      node.head.text shouldBe "partyId1"
      node(1).text shouldBe "partyId2"
    }

    "set empty parties element when the party identification numbers empty" in new SetUp {
      private val response = statusResponseCreator.create(statusResponseEmptyParties)
      private val node = response \\ "parties"

      node.size shouldBe 2
    }

    "no parties element when not present" in new SetUp {
      private val response = statusResponseCreator.create(statusResponseNoParties)
      private val node = response \\ "parties"

      node shouldBe empty
    }
  }

}
