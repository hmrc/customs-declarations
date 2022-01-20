/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.customs.declaration.connectors.NrsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper.{ApiVersionRequestOps, AuthorisedRequestOps, ConversationIdRequestOps, ValidatedHeadersRequestOps}
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.model.{ApiVersion, _}
import uk.gov.hmrc.customs.declaration.services.{AuditingService, DateTimeService, NrsService}
import uk.gov.hmrc.http._
import util.TestData.{TestCspValidatedPayloadRequest, _}

import scala.concurrent.Future

class NrsServiceSpec extends AnyWordSpecLike with MockitoSugar with Matchers{
  private val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val ec = Helpers.stubControllerComponents().executionContext

  trait SetUp {
    protected val mockLogger: DeclarationsLogger = mock[DeclarationsLogger]
    protected val mockDateTimeService: DateTimeService = mock[DateTimeService]
    protected val mockNrsConnector: NrsConnector = mock[NrsConnector]
    protected val mockAuditingService: AuditingService = mock[AuditingService]

    protected lazy val service: NrsService = new NrsService(mockLogger, mockNrsConnector, mockAuditingService, mockDateTimeService)

    protected val cspResponsePayload: NrSubmissionId = nrSubmissionId
    protected val dateTime = new DateTime()

    protected def send(vupr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest, hc: HeaderCarrier = headerCarrier): NrSubmissionId = {
      when(mockDateTimeService.nowUtc()).thenReturn(nrsTimeStamp)
      await(service.send(vupr, hc))
    }
  }

  "NrsService" should {
    "send CSP payload to connector" in new SetUp() {
      val testCspValidatedPayloadRequestWithMinimalHeaders = TestConversationIdRequest.toApiVersionRequest(VersionOne)
        .toValidatedHeadersRequest(TestExtractedHeaders)
        .toCspAuthorisedRequest(Csp(Some(declarantEori), Some(badgeIdentifier), Some(nrsRetrievalValues)))
        .toValidatedPayloadRequest(xmlBody = TestXmlPayload)
      
      when(mockNrsConnector.send(any[NrsPayload], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.successful(cspResponsePayload))

      val result = send(testCspValidatedPayloadRequestWithMinimalHeaders)

      result shouldBe cspResponsePayload

      verify(mockNrsConnector).send(meq(cspNrsPayload), any[ApiVersion])(any[ValidatedPayloadRequest[_]])
    }

    "serialise multiple headers correctly" in new SetUp() {
      when(mockNrsConnector.send(any[NrsPayload], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.successful(cspResponsePayload))

      val result = send(TestCspValidatedPayloadRequestMultipleHeaderValues)

      result shouldBe cspResponsePayload
      verify(mockNrsConnector).send(meq(cspNrsPayloadMultipleHeaderValues), any[ApiVersion])(any[ValidatedPayloadRequest[_]])
    }

    "return failed future when nrs service call fails" in new SetUp() {
      when(mockNrsConnector.send(any[NrsPayload], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(new Exception()))

      val result = intercept[Exception](send())

      result shouldBe a[Exception]
    }

    "audit when nrs returns 5xx error response" in new SetUp() {
      when(mockNrsConnector.send(any[NrsPayload], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(new InternalServerException("internal server")))

      val result = intercept[Exception](send())

      result shouldBe a[Exception]
      verify(mockAuditingService).auditFailedNrs(any[NrsPayload], any[HttpException])(any[ValidatedPayloadRequest[AnyContent]])
      verify(mockAuditingService, times(0)).auditFailedNrs(any[NrsPayload], any[UpstreamErrorResponse])(any[ValidatedPayloadRequest[AnyContent]])
    }

    "DO NOT audit when nrs returns 4xx error response" in new SetUp() {
      when(mockNrsConnector.send(any[NrsPayload], any[ApiVersion])(any[ValidatedPayloadRequest[_]])).thenReturn(Future.failed(new BadRequestException("bad request")))

      val result = intercept[Exception](send())

      result shouldBe a[Exception]
      verify(mockAuditingService, times(0)).auditFailedNrs(any[NrsPayload], any[HttpException])(any[ValidatedPayloadRequest[AnyContent]])
      verify(mockAuditingService, times(0)).auditFailedNrs(any[NrsPayload], any[UpstreamErrorResponse])(any[ValidatedPayloadRequest[AnyContent]])
    }
  }
}

