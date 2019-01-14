/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.mvc.{AnyContentAsXml, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.errorBadRequest
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames.XSubmitterIdentifierHeaderName
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{AuthActionSubmitterHeader, HeaderValidator}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AuthorisedRequest, ValidatedHeadersRequest}
import uk.gov.hmrc.customs.declaration.model.{GoogleAnalyticsValues, VersionOne}
import uk.gov.hmrc.customs.declaration.services.{CustomsAuthService, DeclarationsConfigService}
import uk.gov.hmrc.play.test.UnitSpec
import util.CustomsDeclarationsMetricsTestData._
import util.TestData._
import util.{ApiSubscriptionFieldsTestData, RequestHeaders}

class AuthActionCustomHeaderSpec extends UnitSpec with MockitoSugar with ApiSubscriptionFieldsTestData {

  private val errorResponseSubmitterIdentifierHeaderMissing = errorBadRequest(s"$XSubmitterIdentifierHeaderName header is invalid")

  private lazy val validatedHeadersRequestWithInValidSubmitterIdentifier =
    ValidatedHeadersRequest(conversationId, GoogleAnalyticsValues.Submit, EventStart, VersionOne, clientId, testFakeRequestWithHeader(XSubmitterIdentifierHeaderName, "longer_than_seventeen_characters"))

  trait SetUp {
    val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    val mockGoogleAnalyticsConnector: GoogleAnalyticsConnector = mock[GoogleAnalyticsConnector]
    val mockDeclarationConfigService: DeclarationsConfigService = mock[DeclarationsConfigService]
    val customsAuthService = new CustomsAuthService(mock[AuthConnector], mockGoogleAnalyticsConnector, mockLogger)
    val headerValidator = new HeaderValidator(mockLogger)

    val authActionSubmitterHeader: AuthActionSubmitterHeader = new AuthActionSubmitterHeader(customsAuthService, headerValidator, mockLogger, mockGoogleAnalyticsConnector, mockDeclarationConfigService)
  }

  "AuthActionSubmitterHeader " can {
    "when called" should {
      "throw an error when submitter is present but not valid" in new SetUp {

        private val actual: Either[Result, AuthorisedRequest[AnyContentAsXml]] = await(authActionSubmitterHeader.refine(validatedHeadersRequestWithInValidSubmitterIdentifier))

        actual shouldBe Left(errorResponseSubmitterIdentifierHeaderMissing.XmlResult.withHeaders(RequestHeaders.X_CONVERSATION_ID_NAME -> conversationId.toString))
      }
  }}
}
