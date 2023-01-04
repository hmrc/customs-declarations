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

package integration

import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers
import uk.gov.hmrc.customs.declaration.connectors.CustomsDeclarationsMetricsConnector
import uk.gov.hmrc.customs.declaration.controllers._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{AuthActionSubmitterHeader, HeaderWithContentTypeValidator, _}
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.upscan.FileUploadPayloadValidationAction
import uk.gov.hmrc.customs.declaration.controllers.upscan.FileUploadController
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, _}

class ControllersWiringSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar with Matchers{

  private implicit val ec = Helpers.stubControllerComponents().executionContext
  private lazy val mockSubmissionXmlValidationService = mock[SubmissionXmlValidationService]
  private lazy val mockCancellationXmlValidationService = mock[CancellationXmlValidationService]
  private lazy val mockAmendXmlValidationService = mock[AmendXmlValidationService]
  private lazy val mockArrivalNotificationXmlValidationService = mock[ArrivalNotificationXmlValidationService]
  private lazy val mockFileUploadXmlValidationService = mock[FileUploadXmlValidationService]
  private lazy val mockDeclarationsLogger = mock[DeclarationsLogger]
  private lazy val amendController = app.injector.instanceOf[AmendDeclarationController]
  private lazy val arrivalNotificationController = app.injector.instanceOf[ArrivalNotificationDeclarationController]
  private lazy val submitController = app.injector.instanceOf[SubmitDeclarationController]
  private lazy val cancelController = app.injector.instanceOf[CancelDeclarationController]
  private lazy val fileUploadController = app.injector.instanceOf[FileUploadController]
  private lazy val metricsConnector = app.injector.instanceOf[CustomsDeclarationsMetricsConnector]
  private lazy val mockCustomsAuthService = mock[CustomsAuthService]
  private lazy val mockHeaderWithContentTypeValidator = mock[HeaderWithContentTypeValidator]
  private lazy val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  
  "The correct XmlValidationAction" should {
    "be wired into SubmitDeclarationController" in {
      val action = submitController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new SubmitPayloadValidationAction(mockSubmissionXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into CancelDeclarationController" in {
      val action = cancelController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new CancelPayloadValidationAction(mockCancellationXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.cancel"
    }
    "be wired into AmendDeclarationController" in {
      val action = amendController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new AmendPayloadValidationAction(mockAmendXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into ArrivalNotificationDeclarationController" in {
      val action = arrivalNotificationController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new ArrivalNotificationPayloadValidationAction(mockArrivalNotificationXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into FileUploadController" in {
      val action = fileUploadController.fileUploadPayloadValidationComposedAction.fileUploadPayloadValidationAction

      action.getClass.getSimpleName shouldBe new FileUploadPayloadValidationAction(mockFileUploadXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.fileupload"
    }
  }

  "The correct AuthAction class" should {
    "be wired into SubmitDeclarationController" in {
      val common = submitController.common

      common.authAction.getClass.getSimpleName shouldBe new AuthActionSubmitterHeader(mockCustomsAuthService,
        mockHeaderWithContentTypeValidator, mockDeclarationsLogger, mockDeclarationsConfigService).getClass.getSimpleName
    }
    "be wired into CancelDeclarationController" in {
      val common = cancelController.common

      common.authAction.getClass.getSimpleName shouldBe new AuthActionSubmitterHeader(mockCustomsAuthService,
        mockHeaderWithContentTypeValidator, mockDeclarationsLogger, mockDeclarationsConfigService).getClass.getSimpleName
    }
    "be wired into AmendDeclarationController" in {
      val common = amendController.common

      common.authAction.getClass.getSimpleName shouldBe new AuthActionSubmitterHeader(mockCustomsAuthService,
        mockHeaderWithContentTypeValidator, mockDeclarationsLogger, mockDeclarationsConfigService).getClass.getSimpleName
    }
    "be wired into ArrivalNotificationDeclarationController" in {
      val common = arrivalNotificationController.common

      common.authAction.getClass.getSimpleName shouldBe new AuthActionSubmitterHeader(mockCustomsAuthService,
        mockHeaderWithContentTypeValidator, mockDeclarationsLogger, mockDeclarationsConfigService).getClass.getSimpleName
    }
    "be wired into FileUploadController" in {
      val common = fileUploadController.common

      common.authAction.getClass.getSimpleName shouldBe new AuthAction(mockCustomsAuthService,
        mockHeaderWithContentTypeValidator, mockDeclarationsLogger, mockDeclarationsConfigService).getClass.getSimpleName
    }
  }

  "Metrics logging" should {
    "be enabled for SubmitDeclarationController" in {
      submitController.maybeMetricsConnector shouldBe Some(metricsConnector)
    }
    "be disabled for CancelDeclarationController" in {
      cancelController.maybeMetricsConnector shouldBe None
    }
    "be disabled for AmendDeclarationController" in {
      amendController.maybeMetricsConnector shouldBe None
    }
    "be disabled for ArrivalNotificationDeclarationController" in {
      arrivalNotificationController.maybeMetricsConnector shouldBe None
    }
  }

}
