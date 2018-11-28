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
import uk.gov.hmrc.customs.declaration.connectors.{CustomsDeclarationsMetricsConnector, GoogleAnalyticsConnector}
import uk.gov.hmrc.customs.declaration.controllers._
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services._

class ControllersWiringSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar {

  private lazy val mockSubmissionXmlValidationService = mock[SubmissionXmlValidationService]
  private lazy val mockCancellationXmlValidationService = mock[CancellationXmlValidationService]
  private lazy val mockClearanceXmlValidationService = mock[ClearanceXmlValidationService]
  private lazy val mockAmendXmlValidationService = mock[AmendXmlValidationService]
  private lazy val mockArrivalNotificationXmlValidationService = mock[ArrivalNotificationXmlValidationService]
  private lazy val mockFileUploadXmlValidationService = mock[FileUploadXmlValidationService]
  private lazy val mockBatchFileUploadXmlValidationService = mock[BatchFileUploadXmlValidationService]
  private lazy val mockDeclarationsLogger = mock[DeclarationsLogger]
  private lazy val mockGoogleAnalyticsConnector = mock[GoogleAnalyticsConnector]
  private lazy val clearanceController = app.injector.instanceOf[ClearanceDeclarationController]
  private lazy val amendController = app.injector.instanceOf[AmendDeclarationController]
  private lazy val arrivalNotificationController = app.injector.instanceOf[ArrivalNotificationDeclarationController]
  private lazy val submitController = app.injector.instanceOf[SubmitDeclarationController]
  private lazy val cancelController = app.injector.instanceOf[CancelDeclarationController]
  private lazy val fileUploadController = app.injector.instanceOf[FileUploadController]
  private lazy val batchFileUploadController = app.injector.instanceOf[BatchFileUploadController]
  private lazy val metricsConnector = app.injector.instanceOf[CustomsDeclarationsMetricsConnector]

  "The correct XmlValidationAction" should {
    "be wired into SubmitDeclarationController" in {
      val action = submitController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new SubmitPayloadValidationAction(mockSubmissionXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsConnector).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into CancelDeclarationController" in {
      val action = cancelController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new CancelPayloadValidationAction(mockCancellationXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsConnector).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.cancel"
    }
    "be wired into ClearanceDeclarationController" in {
      val action = clearanceController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new ClearancePayloadValidationAction(mockClearanceXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsConnector).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.clearance"
    }
    "be wired into AmendDeclarationController" in {
      val action = amendController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new AmendPayloadValidationAction(mockAmendXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsConnector).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into ArrivalNotificationDeclarationController" in {
      val action = arrivalNotificationController.payloadValidationAction

      action.getClass.getSimpleName shouldBe new ArrivalNotificationPayloadValidationAction(mockArrivalNotificationXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsConnector).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into FileUploadController" in {
      val action = fileUploadController.fileUploadPayloadValidationComposedAction.fileUploadPayloadValidationAction

      action.getClass.getSimpleName shouldBe new FileUploadPayloadValidationAction(mockFileUploadXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsConnector).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.fileupload"
    }
    "be wired into BatchFileUploadController" in {
      val action = batchFileUploadController.batchFileUploadPayloadValidationComposedAction.batchFileUploadPayloadValidationAction

      action.getClass.getSimpleName shouldBe new BatchFileUploadPayloadValidationAction(mockBatchFileUploadXmlValidationService, mockDeclarationsLogger, mockGoogleAnalyticsConnector).getClass.getSimpleName
      action.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.batchfileupload"
    }
  }

  "Metrics logging" should {
    "be enabled for SubmitDeclarationController" in {
      submitController.maybeMetricsConnector shouldBe Some(metricsConnector)
    }
    "be disabled for CancelDeclarationController" in {
      cancelController.maybeMetricsConnector shouldBe None
    }
    "be disabled for ClearanceDeclarationController" in {
      clearanceController.maybeMetricsConnector shouldBe None
    }
    "be disabled for AmendDeclarationController" in {
      amendController.maybeMetricsConnector shouldBe None
    }
    "be disabled for ArrivalNotificationDeclarationController" in {
      arrivalNotificationController.maybeMetricsConnector shouldBe None
    }
  }

}
