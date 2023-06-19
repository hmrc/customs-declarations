/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.ErrorInternalServerError
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.AuthStatusAction
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.Csp
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedHeadersStatusRequest
import util.TestData._
import util.{AuthConnectorNrsDisabledStubbing, TestData}

import scala.concurrent.ExecutionContext

class AuthStatusActionSpec extends AnyWordSpecLike with MockitoSugar with TableDrivenPropertyChecks with BeforeAndAfterEach with Matchers {

  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private lazy val validatedHeadersRequest: ValidatedHeadersStatusRequest[AnyContentAsXml] = TestValidatedHeadersStatusRequest
  private val mockAuthenticationConnector: AuthConnector = mock[AuthConnector]
  private val mockImportsLogger= mock[DeclarationsLogger]

  trait SetUp extends AuthConnectorNrsDisabledStubbing { // NRS not required yet
    override val mockAuthConnector: AuthConnector = mockAuthenticationConnector
  }

  override protected def beforeEach(): Unit = {
    reset(mockAuthenticationConnector)
  }

  "AuthStatusAction" can {
    "for Declaration Status request" should {

      val authAction = new AuthStatusAction(mockAuthenticationConnector, mockImportsLogger)

      "return AuthorisedRequest for CSP when authorised by auth API" in new SetUp {
        authoriseCsp()

        private val actual = await(authAction.refine(validatedHeadersRequest))

        actual shouldBe Right(validatedHeadersRequest.toAuthorisedRequest(Csp(None, Some(badgeIdentifier), None)))
        verifyCspAuthorisationCalled(1)
      }

      "return ErrorResponse with ConversationId when not authorised by auth API" in new SetUp {
        authoriseCspError()

        private val actual = await(authAction.refine(validatedHeadersRequest))
        actual shouldBe Left(ErrorInternalServerError.XmlResult.withHeaders(CustomHeaderNames.XConversationIdHeaderName -> TestData.conversationIdValue))
        verifyCspAuthorisationCalled(1)
      }
    }
  }
}
