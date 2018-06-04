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
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders.{CancelPayloadValidationAction, ClearancePayloadValidationAction, SubmitPayloadValidationAction}
import uk.gov.hmrc.customs.declaration.controllers.{CancelDeclarationController, ClearanceDeclarationController, SubmitDeclarationController}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.services.{CancellationXmlValidationService, ClearanceXmlValidationService, SubmissionXmlValidationService}

class ControllersWiringTestSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MockitoSugar {

  private lazy val mockSubmissionXmlValidationService = mock[SubmissionXmlValidationService]
  private lazy val mockCancellationXmlValidationService = mock[CancellationXmlValidationService]
  private lazy val mockClearanceXmlValidationService = mock[ClearanceXmlValidationService]
  private lazy val mockDeclarationsLogger = mock[DeclarationsLogger]

  private lazy val clearanceController = app.injector.instanceOf[ClearanceDeclarationController]
  private lazy val submitController = app.injector.instanceOf[SubmitDeclarationController]
  private lazy val cancelController = app.injector.instanceOf[CancelDeclarationController]

  "The correct XmlValidationAction"  should {
    "be wired into SubmitDeclarationController " in {

      submitController.payloadValidationAction.getClass.getSimpleName shouldBe new SubmitPayloadValidationAction(mockSubmissionXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      submitController.payloadValidationAction.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
    "be wired into CancelDeclarationController " in {

      cancelController.payloadValidationAction.getClass.getSimpleName shouldBe new CancelPayloadValidationAction(mockCancellationXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      cancelController.payloadValidationAction.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.cancel"
    }
    "be wired into ClearanceDeclarationController " in {

      clearanceController.payloadValidationAction.getClass.getSimpleName shouldBe new ClearancePayloadValidationAction(mockClearanceXmlValidationService, mockDeclarationsLogger).getClass.getSimpleName
      clearanceController.payloadValidationAction.xmlValidationService.schemaPropertyName shouldBe "xsd.locations.submit"
    }
  }

}
