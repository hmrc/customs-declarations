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

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.config.{ServiceConfig, ServiceConfigProvider}
import uk.gov.hmrc.customs.declaration.connectors.MdgWcoDeclarationConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.{ConversationId, Ids, RequestType, SeqOfHeader}
import uk.gov.hmrc.customs.declaration.services.WSHttp
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.{RequestHeaders, TestData}

import scala.concurrent.{ExecutionContext, Future}

class MdgWcoDeclarationConnectorSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach with Eventually {

  private val mockWsPost = mock[WSHttp]
  private val mockDeclarationsLogger = mock[DeclarationsLogger]
  private val mockServiceConfigProvider = mock[ServiceConfigProvider]

  private val connector = new MdgWcoDeclarationConnector(mockWsPost, mockDeclarationsLogger, mockServiceConfigProvider)

  private val v1Config = ServiceConfig("v1-url", Some("v1-bearer-token"), "v1-default")
  private val v2Config = ServiceConfig("v2-url", Some("v2-bearer-token"), "v2-default")

  private val xml = <xml></xml>
  private implicit val hc: HeaderCarrier = HeaderCarrier()
    .withExtraHeaders(RequestHeaders.API_SUBSCRIPTION_FIELDS_ID_HEADER)

  private val httpException = new NotFoundException("Emulated 404 response from a web call")
  private implicit val ids: Ids = Ids(ConversationId("dummy-conversation-id"), RequestType.Submit)

  override protected def beforeEach() {
    reset(mockWsPost, mockDeclarationsLogger, mockServiceConfigProvider)
    when(mockServiceConfigProvider.getConfig("wco-declaration")).thenReturn(v1Config)
    when(mockServiceConfigProvider.getConfig("v2.wco-declaration")).thenReturn(v2Config)
  }

  private val year = 2017
  private val monthOfYear = 7
  private val dayOfMonth = 4
  private val hourOfDay = 13
  private val minuteOfHour = 45
  private val date = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, DateTimeZone.UTC)

  private val httpFormattedDate = "Tue, 04 Jul 2017 13:45:00 UTC"

  private val correlationId = UUID.randomUUID()

  "MdgWcoDeclarationConnector" can {

    "when making a successful request" should {

      "pass URL from config" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        verify(mockWsPost).POSTString(ameq(v1Config.url), anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "pass the xml in the body" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        verify(mockWsPost).POSTString(anyString, ameq(xml.toString()), any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext])
      }

      "set the content type header" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain(HeaderNames.CONTENT_TYPE -> MimeTypes.XML)
      }

      "set the accept header" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain(HeaderNames.ACCEPT -> MimeTypes.XML)
      }

      "set the date header" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain(HeaderNames.DATE -> httpFormattedDate)
      }

      "set the X-Forwarded-Host header" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain(HeaderNames.X_FORWARDED_HOST -> "MDTP")
      }

      "set the X-Correlation-Id header" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        awaitRequest

        val headersCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        verify(mockWsPost).POSTString(anyString, anyString, any[SeqOfHeader])(
          any[HttpReads[HttpResponse]](), headersCaptor.capture(), any[ExecutionContext])
        headersCaptor.getValue.extraHeaders should contain("X-Correlation-ID" -> correlationId.toString)
      }

      "prefix the config key with the prefix if passed" in {
        returnResponseForRequest(Future.successful(mock[HttpResponse]))

        await(connector.send(xml, date, correlationId, Some("v2")))

        verify(mockServiceConfigProvider).getConfig("v2.wco-declaration")
      }
    }

    "when making an failing request" should {
      "propagate an underlying error when MDG call fails with a non-http exception" in {
        returnResponseForRequest(Future.failed(TestData.emulatedServiceFailure))

        val caught = intercept[TestData.EmulatedServiceFailure] {
          awaitRequest
        }
        caught shouldBe TestData.emulatedServiceFailure
      }

      "wrap an underlying error when MDG call fails with an http exception" in {
        returnResponseForRequest(Future.failed(httpException))

        val caught = intercept[RuntimeException] {
          awaitRequest
        }
        caught.getCause shouldBe httpException
      }
    }
  }

  private def awaitRequest = {
    await(connector.send(xml, date, correlationId))
  }

  private def returnResponseForRequest(eventualResponse: Future[HttpResponse]) = {
    when(mockWsPost.POSTString(anyString, anyString, any[SeqOfHeader])(
      any[HttpReads[HttpResponse]](), any[HeaderCarrier](), any[ExecutionContext]))
      .thenReturn(eventualResponse)
  }
}
