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

package unit.connectors

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import play.api.libs.json.{JsValue, Writes}
import play.api.mvc.AnyContentAsXml
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AnalyticsValuesAndConversationIdRequest, HasConversationId, ValidatedPayloadRequest}
import uk.gov.hmrc.customs.declaration.model.{GoogleAnalyticsConfig, GoogleAnalyticsRequest, GoogleAnalyticsValues}
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import util.CustomsDeclarationsMetricsTestData._
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData
import util.TestData.{TestFakeRequest, conversationId}

import scala.concurrent.{ExecutionContext, Future}

class GoogleAnalyticsConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val mockHttpClient = mock[HttpClient]
  private val mockCdsLogger = mock[CdsLogger]
  private val declarationsLogger = mock[DeclarationsLogger]
  private val mockConfigService = mock[DeclarationsConfigService]

  private val gaEnabled = true
  private val url = "the-url"
  private val gaTrackingId: String = "real-tracking-id"
  private val gaClientId: String = "555"
  private val gaEventValue = "10"
  private val eventName: String = "event-name"
  private val eventLabel: String = "event-label"

  private implicit val vpr: ValidatedPayloadRequest[AnyContentAsXml] = TestData.TestCspValidatedPayloadRequest

  private lazy val connector = new GoogleAnalyticsConnector(
    mockHttpClient,
    declarationsLogger,
    mockConfigService
  )

  override def beforeEach(): Unit = {
    reset(mockConfigService, mockCdsLogger, mockHttpClient)
    when(mockConfigService.googleAnalyticsConfig).thenReturn(GoogleAnalyticsConfig(gaEnabled, url, gaTrackingId, gaClientId, gaEventValue))
    when(mockHttpClient.POST(any[String](), any[JsValue](), any[Seq[(String, String)]]())(any[Writes[JsValue]](), any[HttpReads[HttpResponse]](), any[HeaderCarrier], any[ExecutionContext]()))
      .thenReturn(Future.successful(mock[HttpResponse]))
  }

  private val emulatedHttpVerbsException = new RuntimeException("Something has gone wrong....")

  "GoogleAnalyticsSenderConnector when enabled" should {

    "POST valid payload" in {

      await(connector.send(eventName, eventLabel))
      val expectedBody = GoogleAnalyticsRequest(s"v=1&t=event&tid=$gaTrackingId&cid=$gaClientId&ec=CDS&ea=$eventName&el=$eventLabel&ev=$gaEventValue")
      val requestCaptor: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

      verify(mockHttpClient).POST(ArgumentMatchers.eq(url), requestCaptor.capture(), any[Seq[(String, String)]]())(
        any(), any(), any(), any())
      requestCaptor.getValue shouldBe expectedBody
    }

    "POST valid headers" in {
      val expectedHeaders = Seq(
        (ACCEPT, MimeTypes.JSON),
        (CONTENT_TYPE, MimeTypes.JSON))

      await(connector.send(eventName, eventLabel))

      verify(mockHttpClient).POST(ArgumentMatchers.eq(url), any(), meq(expectedHeaders))(any(), any(), any(), any())
    }

    "not propagate exception, log it correctly" in {
      when(mockHttpClient.POST(any(), any(), any())(any[Writes[JsValue]](), any[HttpReads[HttpResponse]](), any[HeaderCarrier], any[ExecutionContext]()))
        .thenReturn(Future.failed(emulatedHttpVerbsException))

      await(connector.send(eventName, eventLabel))

      PassByNameVerifier(declarationsLogger, "error")
        .withByNameParam(s"Call to GoogleAnalytics sender service failed. POST url= $url, eventName= $eventName, eventLabel= $eventLabel, reason= ${emulatedHttpVerbsException.getMessage}")
        .withParamMatcher(any[HasConversationId])
        .verify()
    }

  }

  "GoogleAnalyticsSenderConnector when disabled in configuration" should {

    "not POST valid payload" in {
      when(mockConfigService.googleAnalyticsConfig).thenReturn(GoogleAnalyticsConfig(enabled = false, url, gaTrackingId, gaClientId, gaEventValue))

      await(connector.send(eventName, eventLabel))

      verifyZeroInteractions(mockHttpClient)
    }

    "not POST valid headers" in {
      when(mockConfigService.googleAnalyticsConfig).thenReturn(GoogleAnalyticsConfig(enabled = false, url, gaTrackingId, gaClientId, gaEventValue))

      await(connector.send(eventName, eventLabel))

      verifyZeroInteractions(mockHttpClient)
    }

  }

  "GoogleAnalyticsSenderConnector when disabled by endpoint" should {

    "not POST payload" in {
      implicit val vpr = AnalyticsValuesAndConversationIdRequest(conversationId, mock[GoogleAnalyticsValues], EventStart, TestFakeRequest)
      when(vpr.analyticsValues.enabled).thenReturn(false)

      await(connector.send(eventName, eventLabel))

      verifyZeroInteractions(mockHttpClient)
    }

  }

}
