/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, StatusResponseFilterService}
import util.StatusTestXMLData.{DeclarationType, ImportTradeMovementType, generateDeclarationStatusResponse, generateValidStatusResponseWithMultiplePartiesOnly}
import util.TestData.{date, dateString}

import scala.xml.NodeSeq

class DeclarationStatusResponseFilterServiceSpec extends AnyWordSpecLike with MockitoSugar with Matchers {

  private val acceptanceDateVal = DateTime.now(DateTimeZone.UTC)

  trait SetUp {

    val mockDeclarationsLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockDeclarationsConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]

    val service = new StatusResponseFilterService(mockDeclarationsLogger, mockDeclarationsConfigService)

    def createStatusResponseWithAllValues(): NodeSeq = service.transform(generateDeclarationStatusResponse(acceptanceOrCreationDate = acceptanceDateVal))
  }

  "Status Response Filter Service" should {

    "create the version number" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "VersionID"

      node.text shouldBe "0"
    }

    "create the mrn" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \ "Declaration" \ "ID"

      node.text shouldBe "mrn"
    }

    "create the creation date" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "CreationDateTime" \\ "DateTimeString"

      node.text shouldBe "2001-12-17T09:30:47Z"
      node.head.attribute("formatCode").get.text shouldBe "string"
    }

    "create the goods item count" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "GoodsItemQuantity"

      node.text shouldBe "2"
      node.head.attribute("unitType").get.text shouldBe "101"
    }

    "create the type code" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "TypeCode"

      node.text shouldBe ImportTradeMovementType + DeclarationType
    }

    "create the TotalPackageQuantity" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "TotalPackageQuantity"

      node.text shouldBe "3"
    }

    "create the acceptance date" in new SetUp {
      private val response = service.transform(generateDeclarationStatusResponse(acceptanceOrCreationDate = date))
      private val node = response \\ "AcceptanceDateTime" \\ "DateTimeString"
      node.head.text shouldBe dateString
    }

    "create the TB party identification number" in new SetUp {
      private val response = createStatusResponseWithAllValues()
      private val node = response \\ "Submitter" \ "ID"

      node.head.text shouldBe "123456"
    }

    "create the party identification number when there is one with TB type and the rest are not" in new SetUp{
      private val response = service.transform(generateValidStatusResponseWithMultiplePartiesOnly)
      private val node = response \\ "Submitter" \ "ID"

      node.head.text shouldBe "1"
    }

    "not create acceptance date when not provided" in new SetUp {
      private val response = service.transform(generateValidStatusResponseWithMultiplePartiesOnly)
      private val node = response \\ "AcceptanceDateTime" \\ "DateTimeString"

      node shouldBe empty
    }

    "not create submitter id when not provided" in new SetUp {
      private val response = service.transform(generateDeclarationStatusResponse(acceptanceOrCreationDate = acceptanceDateVal, partyType = "TT"))
      private val node = response \\ "Submitter" \ "ID"

      node shouldBe empty
    }

  }
}
