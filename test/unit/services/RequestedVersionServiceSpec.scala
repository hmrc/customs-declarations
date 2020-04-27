/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.http.MimeTypes
import uk.gov.hmrc.customs.declaration.model.RequestedVersion
import uk.gov.hmrc.customs.declaration.services.RequestedVersionService
import util.UnitSpec

class RequestedVersionServiceSpec extends UnitSpec {

  private val requestedVersionService: RequestedVersionService = new RequestedVersionService()

  private val acceptHeaderV1 = "application/vnd.hmrc.1.0+xml"
  private val acceptHeaderV2 = "application/vnd.hmrc.2.0+xml"

  private val version1 = RequestedVersion(versionNumber = "1.0", configPrefix = None)
  private val version2 = RequestedVersion(versionNumber = "2.0", configPrefix = Some("v2"))

  "RequestedVersionService" should {
    "return valid Accept header values" in {
      requestedVersionService.validAcceptHeaders should contain theSameElementsAs Seq(acceptHeaderV1, acceptHeaderV2)
    }

    s"return v1 for Accept header $acceptHeaderV1" in {
      requestedVersionService.getVersionByAcceptHeader(Some(acceptHeaderV1)) shouldBe Some(version1)
    }

    s"return v2 for Accept header $acceptHeaderV2" in {
      requestedVersionService.getVersionByAcceptHeader(Some(acceptHeaderV2)) shouldBe Some(version2)
    }

    "return None for Accept header application/xml" in {
      requestedVersionService.getVersionByAcceptHeader(Some("application/xml")) shouldBe None
    }

    "return None for any other Accept header" in {
      requestedVersionService.getVersionByAcceptHeader(Some(MimeTypes.JSON)) shouldBe None
    }

    "return None for an empty Accept header" in {
      requestedVersionService.getVersionByAcceptHeader(None) shouldBe None
    }
  }

}
