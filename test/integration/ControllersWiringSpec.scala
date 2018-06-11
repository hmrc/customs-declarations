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

package integration

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.customs.declaration.controllers._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services._

class ControllersWiringSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar {

  private lazy val mockSubmissionXmlValidationService = mock[SubmissionXmlValidationService]
  private lazy val mockCancellationXmlValidationService = mock[CancellationXmlValidationService]
  private lazy val mockClearanceXmlValidationService = mock[ClearanceXmlValidationService]
  private lazy val mockAmendXmlValidationService = mock[AmendXmlValidationService]
  private lazy val mockFileUploadXmlValidationService = mock[FileUploadXmlValidationService]
  private lazy val mockDeclarationsLogger = mock[DeclarationsLogger]
  private lazy val mockGoogleAnalyticsService = mock[GoogleAnalyticsService]
  private lazy val clearanceController = app.injector.instanceOf[ClearanceDeclarationController]
  private lazy val amendController = app.injector.instanceOf[AmendDeclarationController]
  private lazy val submitController = app.injector.instanceOf[SubmitDeclarationController]
  private lazy val cancelController = app.injector.instanceOf[CancelDeclarationController]
  private lazy val fileUploadController = app.injector.instanceOf[FileUploadController]

  "The correct XmlValidationAction"  should {
    "be wired into SubmitDeclarationController" in {
      val action = submitController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new SubmitPayloadValidationAction(mockSubmissionXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsService).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into CancelDeclarationController" in {
      val action = cancelController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new CancelPayloadValidationAction(mockCancellationXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsService).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.cancel"
    }
    "be wired into ClearanceDeclarationController" in {
      val action = clearanceController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new ClearancePayloadValidationAction(mockClearanceXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsService).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into AmendDeclarationController" in {
      val action = amendController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new AmendPayloadValidationAction(mockAmendXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into FileUploadController" in {
      val action = fileUploadController.fileUploadPayloadValidationComposedAction.fileUploadPayloadValidationAction

      action.getClass.getSimpleName shouldBe new FileUploadPayloadValidationAction(mockFileUploadXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsService).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.fileupload"
    }
  }

}
