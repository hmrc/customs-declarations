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

package unit.controllers

import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames.{ACCEPT, CONTENT_TYPE}
import play.api.mvc.Results._
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.declaration.controllers.HeaderValidator
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.RequestedVersionService
import uk.gov.hmrc.play.test.UnitSpec
import util.RequestHeaders._

class HeaderValidatorSpec extends UnitSpec with BeforeAndAfterAll with MockitoSugar {

  private val mockRequestedVersionService = mock[RequestedVersionService]

  private val validator = new HeaderValidator {
    override val declarationsLogger: DeclarationsLogger = mock[DeclarationsLogger]
    override val requestedVersionService: RequestedVersionService = mockRequestedVersionService
  }

  private def requestWithHeaders(headers: Map[String, String]) =
    FakeRequest().withHeaders(headers.toSeq: _*)

  override protected def beforeAll(): Unit = {
    when(mockRequestedVersionService.validAcceptHeaders).thenReturn(Set(ACCEPT_HMRC_XML_V1_VALUE, ACCEPT_HMRC_XML_V2_VALUE))
  }

  "HeaderValidatorAction" should {
    "return processing result when accept header is v1" in{
      await(validator.validateAccept()(requestWithHeaders(ValidHeaders - ACCEPT + ACCEPT_HMRC_XML_V1_HEADER))) shouldBe None
    }

    "return processing result when accept header is v2" in{
      await(validator.validateAccept()(requestWithHeaders(ValidHeaders - ACCEPT + ACCEPT_HMRC_XML_V2_HEADER))) shouldBe None
    }

    "return processing result when request headers contain valid values" in {
      await(validator.validateAccept()(requestWithHeaders(ValidHeaders))) shouldBe None
    }

    "return processing result when Content-Type header contains charset" in {
      await(validator.validateContentType()(requestWithHeaders(ValidHeaders - CONTENT_TYPE + CONTENT_TYPE_HEADER_CHARSET))) shouldBe None
    }

    "return Error result when the Accept header does not exist" in {
      await(validator.validateAccept()(requestWithHeaders(ValidHeaders - ACCEPT))) shouldBe Some(ErrorAcceptHeaderInvalid)
    }

    "return Error result when Accept header does not contain expected value" in {
      await(validator.validateAccept()(requestWithHeaders(ValidHeaders + ACCEPT_HEADER_INVALID))) shouldBe Some(ErrorAcceptHeaderInvalid)
    }

    "return Error result when the Content-Type header does not exist" in {
      await(validator.validateContentType()(requestWithHeaders(ValidHeaders - CONTENT_TYPE))) shouldBe Some(ErrorContentTypeHeaderInvalid)
    }

    "return Error result when Content-Type header does not contain expected value" in {
      await(validator.validateContentType()(requestWithHeaders(ValidHeaders + CONTENT_TYPE_HEADER_INVALID))) shouldBe Some(ErrorContentTypeHeaderInvalid)
    }

  }
}
