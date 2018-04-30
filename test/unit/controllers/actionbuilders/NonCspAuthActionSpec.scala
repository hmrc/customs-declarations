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
import play.api.http.Status
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.Result
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.UnauthorizedCode
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.NonCspAuthAction
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AuthorisedRequest
import uk.gov.hmrc.play.test.UnitSpec
import util.{AuthConnectorStubbing, RequestHeaders}
import util.TestData._

class NonCspAuthActionSpec extends UnitSpec with MockitoSugar {

  private type EitherResultOrAuthRequest[A] = Either[Result, AuthorisedRequest[A]]

  private val errorResponseUnauthorisedGeneral =
    ErrorResponse(Status.UNAUTHORIZED, UnauthorizedCode, "Unauthorised request")
  private lazy val errorResponseEoriNotFoundInCustomsEnrolment =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "EORI number not found in Customs Enrolment")

  trait SetUp extends AuthConnectorStubbing {
    val mockExportsLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val nonCspAuthAction: NonCspAuthAction = new NonCspAuthAction(mockAuthConnector, mockExportsLogger)
  }

  "NonCspAuthAction" should {
    "Authorise Non CSP when authorised by auth API " in new SetUp {
      authoriseNonCsp(Some(declarantEori))

      private val actual = await(nonCspAuthAction.refine(TestUnAuthorisedRequestNoBadge))

      actual shouldBe Right(TestNonCspAuthorisedRequest)
    }

    "pass through request when already authorised" in new SetUp {
      authoriseNonCsp(Some(declarantEori))

      private val actual = await(nonCspAuthAction.refine(TestCspAuthorisedRequest))

      actual shouldBe Right(TestCspAuthorisedRequest)
    }

    "Return 401 when authorised by auth API but Eori not exists" in new SetUp {
      authoriseNonCsp(maybeEori = None)

      private val actual = await(nonCspAuthAction.refine(TestUnAuthorisedRequestNoBadge))

      actual shouldBe Left(errorResponseEoriNotFoundInCustomsEnrolment.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.value))
    }

    "Return 401 when not authorised as NonCsp" in new SetUp {
      unauthoriseNonCspOnly()

      private val actual = await(nonCspAuthAction.refine(TestUnAuthorisedRequestNoBadge))

      actual shouldBe Left(errorResponseUnauthorisedGeneral.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.value))
    }

    "propagate exceptional errors in auth API" in new SetUp {
      authoriseNonCspOnlyError()

      private val caught = intercept[Throwable](await(nonCspAuthAction.refine(TestUnAuthorisedRequestNoBadge)))

      caught shouldBe emulatedServiceFailure
    }
  }
}
