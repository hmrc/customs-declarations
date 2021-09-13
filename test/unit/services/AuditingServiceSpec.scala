/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatest.concurrent.Eventually
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsXml
import play.api.test.Helpers
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ValidatedPayloadRequest
import uk.gov.hmrc.customs.declaration.services.{AuditingService, DeclarationsConfigService}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import util.UnitSpec
import unit.logging.StubDeclarationsLogger
import util.TestData.{TestCspValidatedPayloadRequest, nrsConfigEnabled, nrsPayload}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AuditingServiceSpec extends WordSpec with MockitoSugar with BeforeAndAfterEach with Eventually with Matchers{

  private implicit val ec = Helpers.stubControllerComponents().executionContext
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 5 seconds)

  private val stubNotificationLogger = new StubDeclarationsLogger(mock[CdsLogger])
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val mockAuditConnector = mock[AuditConnector]

  override def beforeEach(): Unit = {
    org.mockito.Mockito.reset(mockDeclarationsConfigService)
    org.mockito.Mockito.reset(mockAuditConnector)
  }

  val auditingService = new AuditingService(stubNotificationLogger, mockDeclarationsConfigService, mockAuditConnector)

  "AuditingService" should {

    "call audit connector with correct payload for auditing failed notification for HttpException" in {

      val mockAuditResult = mock[AuditResult]
      implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(mockAuditResult))
      when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)

      auditingService.auditFailedNrs(nrsPayload, new BadRequestException("bad request"))

      eventually {
        verify(mockAuditConnector).sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
        val actualExtendedDataEvent: ExtendedDataEvent = captor.getValue
        actualExtendedDataEvent.auditSource shouldBe "customs-declaration-submission"
        actualExtendedDataEvent.auditType shouldBe "DeclarationNotificationOutboundCall"
        actualExtendedDataEvent.tags("path") shouldBe nrsConfigEnabled.nrsUrl
        actualExtendedDataEvent.tags("transactionName") shouldBe  "Unsuccessful Non-Repudiation Submission API call"
        actualExtendedDataEvent.tags("x-request-id") shouldBe vpr.headers.get("X-Request-Id").getOrElse("")
        actualExtendedDataEvent.tags("clientIP") shouldBe vpr.headers.get("X-Forwarded-For").getOrElse("")
        actualExtendedDataEvent.tags("clientPort") shouldBe vpr.headers.get("X-Forwarded-Port").getOrElse("")
        (actualExtendedDataEvent.detail \ "errorCode" ).as[String] shouldBe "400"
        (actualExtendedDataEvent.detail \ "errorMessage" ).as[String] shouldBe "bad request"
        (actualExtendedDataEvent.detail \ "businessId" ).as[String] shouldBe nrsPayload.metadata.businessId
        (actualExtendedDataEvent.detail \ "noteableEvent" ).as[String] shouldBe nrsPayload.metadata.notableEvent
        (actualExtendedDataEvent.detail \ "x-conversation-Id" ).as[String] shouldBe vpr.conversationId.toString
        (actualExtendedDataEvent.detail \ "userSubmissionTimestamp" ).as[String] shouldBe nrsPayload.metadata.userSubmissionTimestamp
        (actualExtendedDataEvent.detail \ "identityData" ).as[JsValue] shouldBe Json.toJson(nrsPayload.metadata.identityData)
        actualExtendedDataEvent.eventId should fullyMatch regex """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"""
        actualExtendedDataEvent.generatedAt.toString() should not be empty
      }
    }

    "call audit connector with correct payload for auditing failed notification for Upstream5xxResponse" in {

      val mockAuditResult = mock[AuditResult]
      implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestCspValidatedPayloadRequest
      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(mockAuditResult))
      when(mockDeclarationsConfigService.nrsConfig).thenReturn(nrsConfigEnabled)

      auditingService.auditFailedNrs(nrsPayload, UpstreamErrorResponse("internal server", 500, 502))

      eventually {
        verify(mockAuditConnector).sendExtendedEvent(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
        val actualExtendedDataEvent: ExtendedDataEvent = captor.getValue
        actualExtendedDataEvent.auditSource shouldBe "customs-declaration-submission"
        actualExtendedDataEvent.auditType shouldBe "DeclarationNotificationOutboundCall"
        actualExtendedDataEvent.tags("path") shouldBe nrsConfigEnabled.nrsUrl
        actualExtendedDataEvent.tags("transactionName") shouldBe  "Unsuccessful Non-Repudiation Submission API call"
        actualExtendedDataEvent.tags("x-request-id") shouldBe vpr.headers.get("X-Request-Id").getOrElse("")
        actualExtendedDataEvent.tags("clientIP") shouldBe vpr.headers.get("X-Forwarded-For").getOrElse("")
        actualExtendedDataEvent.tags("clientPort") shouldBe vpr.headers.get("X-Forwarded-Port").getOrElse("")
        (actualExtendedDataEvent.detail \ "errorCode" ).as[String] shouldBe "500"
        (actualExtendedDataEvent.detail \ "errorMessage" ).as[String] shouldBe "internal server"
        (actualExtendedDataEvent.detail \ "businessId" ).as[String] shouldBe nrsPayload.metadata.businessId
        (actualExtendedDataEvent.detail \ "noteableEvent" ).as[String] shouldBe nrsPayload.metadata.notableEvent
        (actualExtendedDataEvent.detail \ "x-conversation-Id" ).as[String] shouldBe vpr.conversationId.toString
        (actualExtendedDataEvent.detail \ "userSubmissionTimestamp" ).as[String] shouldBe nrsPayload.metadata.userSubmissionTimestamp
        (actualExtendedDataEvent.detail \ "identityData" ).as[JsValue] shouldBe Json.toJson(nrsPayload.metadata.identityData)
        actualExtendedDataEvent.eventId should fullyMatch regex """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"""
        actualExtendedDataEvent.generatedAt.toString() should not be empty
      }
    }
  }
}
