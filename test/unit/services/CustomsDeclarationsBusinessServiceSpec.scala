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

package unit.services

import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.xml.sax.SAXException
import play.api.test.Helpers._
import uk.gov.hmrc.customs.api.common.controllers.{ErrorResponse, ResponseContents}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.RequestedVersion
import uk.gov.hmrc.customs.declaration.services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class CustomsDeclarationsBusinessServiceSpec extends UnitSpec with Matchers with MockitoSugar with BeforeAndAfterEach with TableDrivenPropertyChecks {

  private val mockDeclarationsLogger = mock[DeclarationsLogger]
  private val mockCommunicationService = mock[CommunicationService]
  private val mockXmlValidationService = mock[XmlValidationService]

  private val service = new CustomsDeclarationsBusinessService(mockDeclarationsLogger,
    mockCommunicationService,
    mockXmlValidationService)

  private implicit val mockHeaderCarrier: HeaderCarrier = mock[HeaderCarrier]
  private implicit val mockVersion: RequestedVersion = mock[RequestedVersion]

  private val xmlValidationErrorText = "cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'."
  private val xmlValidationException = new SAXException(xmlValidationErrorText)

  private val xmlValidationErrorResponse = ErrorResponse(BAD_REQUEST, errorCode = "BAD_REQUEST",
    message = "Payload is not valid according to schema",
    ResponseContents(code = "xml_validation_error", message = xmlValidationErrorText))

  override protected def beforeEach() {
    reset(mockDeclarationsLogger, mockCommunicationService, mockXmlValidationService)
    when(mockXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext])).thenReturn(())
    when(mockCommunicationService.prepareAndSend(any[NodeSeq], any[RequestedVersion])(any[HeaderCarrier]())).thenReturn(ids)
  }

  private val allSubmissionModes = Table(("description", "xml submission thunk with service"),
    ("CSP",     service.authorisedCspSubmission(_: NodeSeq)),
    ("non-CSP", service.authorisedNonCspSubmission(_: NodeSeq))
  )

  forAll(allSubmissionModes) { case (submissionMode, xmlSubmission) =>

    s"CustomsDeclarationsBusinessService when $submissionMode ia submitting" should {

      "validate incoming xml" in {
        testSubmitResult(xmlSubmission(ValidXML)) { result =>
          await(result)
          verify(mockXmlValidationService).validate(ameq(ValidXML))(any[ExecutionContext])
        }
      }

      "send valid xml to communication service" in {
        testSubmitResult(xmlSubmission(ValidXML)) { result =>
          await(result)
          verify(mockCommunicationService).prepareAndSend(ameq(ValidXML), any[RequestedVersion])(ameq(mockHeaderCarrier))
        }
      }

      "implicitly pass requested api version to communicationService" in {
        testSubmitResult(xmlSubmission(ValidXML)) { result =>
          await(result)
          verify(mockCommunicationService).prepareAndSend(any[NodeSeq], ameq(mockVersion))(ameq(mockHeaderCarrier))
        }
      }

      "return conversationId and fieldsId for a processed valid request" in {
        testSubmitResult(xmlSubmission(ValidXML)) { result =>
          await(result) shouldBe Right(ids)
        }
      }

      "prevent from sending an invalid xml returning xml errors" in {
        when(mockXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext]))
          .thenReturn(Future.failed(xmlValidationException))

        testSubmitResult(xmlSubmission(ValidXML)) { result =>
          await(result) shouldBe Left(xmlValidationErrorResponse)
          verifyZeroInteractions(mockCommunicationService)
        }
      }

      "propagate the error when xml validation fails with a system error" in {
        when(mockXmlValidationService.validate(any[NodeSeq])(any[ExecutionContext]))
          .thenReturn(Future.failed(emulatedServiceFailure))

        testSubmitResult(xmlSubmission(InvalidXML)) { result =>
          intercept[EmulatedServiceFailure](await(result)) shouldBe emulatedServiceFailure
          verifyZeroInteractions(mockCommunicationService)
        }
      }

      "propagate the error for a valid request when downstream communication fails" in {
        when(mockCommunicationService.prepareAndSend(any[NodeSeq](), any[RequestedVersion])(any[HeaderCarrier]()))
          .thenReturn(Future.failed(emulatedServiceFailure))

        testSubmitResult(xmlSubmission(ValidXML)) { result =>
          intercept[EmulatedServiceFailure](await(result)) shouldBe emulatedServiceFailure
        }
      }

    }
  }


  private def testSubmitResult(xmlSubmission: Future[ProcessingResult])
                              (test: Future[ProcessingResult] => Unit) {
    test.apply(xmlSubmission)
  }

}
