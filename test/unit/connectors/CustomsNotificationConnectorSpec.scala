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

import org.mockito.ArgumentMatchers.{eq => ameq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.mvc.Http.HeaderNames.{ACCEPT, AUTHORIZATION, CONTENT_TYPE}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.config.ServicesConfig
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.connectors.CustomsNotificationConnector
import uk.gov.hmrc.customs.declaration.model.DeclarationsConfig
import uk.gov.hmrc.customs.declaration.services.{CustomsNotification, DeclarationsConfigService}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec
import util.CustomsDeclarationsExternalServicesConfig.ApiSubscriptionFieldsContext
import util.ExternalServicesConfig.{Host, Port}
import util.MockitoPassByNameHelper.PassByNameVerifier

import scala.concurrent.Future

class CustomsNotificationConnectorSpec extends UnitSpec
  with MockitoSugar
  with BeforeAndAfterEach {

  private val mockHttpClient = mock[HttpClient]
  private val mockLogger = mock[CdsLogger]
  private val mockConfigs = mock[ServicesConfig]
  private val mockDeclarationsConfigService = mock[DeclarationsConfigService]
  private val mockDeclarationsConfig = mock[DeclarationsConfig]

  private lazy val connector = new CustomsNotificationConnector(mockHttpClient, mockLogger, mockDeclarationsConfigService)

  private val basicAuthToken = "basic-Auth-Token"

  private val customsNotification = CustomsNotification("client-subscription-id", UUID.randomUUID(), <some>xml</some>)

  private val expectedUrl = s"http://$Host:$Port$ApiSubscriptionFieldsContext"
  private val expectedHeaders: Map[String, String] = Map(
    "X-CDS-Client-ID" -> customsNotification.clientSubscriptionId,
    "X-Conversation-ID" -> customsNotification.conversationId.toString,
    CONTENT_TYPE -> s"${MimeTypes.XML}; charset=UTF-8",
    ACCEPT -> MimeTypes.XML,
    AUTHORIZATION -> s"Basic $basicAuthToken"
  )

  override protected def beforeEach() {
    reset(mockLogger, mockHttpClient, mockConfigs, mockDeclarationsConfigService)

    when(mockDeclarationsConfigService.declarationsConfig).thenReturn(mockDeclarationsConfig)
    when(mockDeclarationsConfig.customsNotificationBaseBaseUrl).thenReturn(expectedUrl)
    when(mockDeclarationsConfig.customsNotificationBearerToken).thenReturn(basicAuthToken)
  }

  "CustomsNotificationConnector" should {

    "make correct request" in {
      when(mockHttpClient.POSTString[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      await(connector.send(customsNotification))

      verify(mockHttpClient).POSTString(
        ameq(expectedUrl),
        ameq(s"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" + customsNotification.payload.toString()),
        ameq(expectedHeaders.toSeq)
      )(any(), any(), any())
    }

    "log the error in case request to notification service fails" in {
      val exception = new RuntimeException("An error happened....")
      when(mockHttpClient.POSTString[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.failed(exception))

      await(connector.send(customsNotification))

      val expectedLog = s"[conversationId=${customsNotification.conversationId}][clientSubscriptionId=${customsNotification.clientSubscriptionId}] FileUploadScanNotificationHandler -> Request to Customs Notification failed."
      PassByNameVerifier(mockLogger, "error")
        .withByNameParam(expectedLog)
        .withByNameParam(exception)
        .verify()
    }

  }
}
