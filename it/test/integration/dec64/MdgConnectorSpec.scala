/*
 * Copyright 2026 HM Revenue & Customs
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

package integration.dec64

import com.github.tomakehurst.wiremock.http.Fault
import integration.IntegrationTestSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import uk.gov.hmrc.customs.declaration.connectors.dec64.*
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.xml.MdgRequestSerialiser
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.CustomsDeclarationsExternalServicesConfig.MdgDec64Context
import util.TransmissionRequestTestData.TransmissionRequestOne
import util.ExternalServicesConfig.{Host, Port}
import util.externalservices.MdgDec64Service

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class MdgConnectorSpec extends IntegrationTestSpec with GuiceOneAppPerSuite with MdgDec64Service {

  private val BearerToken = "dec64-it-secret-token"
  private val RequestTimeoutMillis = 1000

  private class RecordingCdsLogger extends CdsLogger(new ServicesConfig(Configuration.empty)) {
    val messages: ListBuffer[String] = ListBuffer.empty
    private def record(msg: => String): Unit = messages.synchronized(messages += msg)
    override def debug(msg: => String): Unit = record(msg)
    override def debug(msg: => String, e: => Throwable): Unit = record(msg)
    override def info(msg: => String): Unit = record(msg)
    override def info(msg: => String, e: => Throwable): Unit = record(msg)
    override def warn(msg: => String): Unit = record(msg)
    override def warn(msg: => String, e: => Throwable): Unit = record(msg)
    override def error(msg: => String): Unit = record(msg)
    override def error(msg: => String, e: => Throwable): Unit = record(msg)
  }

  private val recordingLogger = new RecordingCdsLogger

  override implicit lazy val app: Application =
    GuiceApplicationBuilder()
      .overrides(bind[CdsLogger].toInstance(recordingLogger))
      .configure(Map(
        "auditing.enabled" -> false,
        "play.ws.timeout.request" -> s"$RequestTimeoutMillis milliseconds",
        "mdg.endpoint" -> s"http://$Host:$Port$MdgDec64Context",
        "mdg.authorizationToken" -> BearerToken
      )).build()

  private lazy val connector = app.injector.instanceOf[MdgConnector]
  private lazy val serialiser = app.injector.instanceOf[MdgRequestSerialiser]

  override protected def beforeAll(): Unit = startMockServer()

  override protected def beforeEach(): Unit = recordingLogger.messages.synchronized(recordingLogger.messages.clear())

  override protected def afterEach(): Unit = resetMockServer()

  override protected def afterAll(): Unit = stopMockServer()

  private def sendValidRequest(): Future[MdgRequestResult] =
    connector.requestTransmission(TransmissionRequestOne)(using HeaderCarrier())

  private def tokenWasNeverLogged(): Unit = {
    recordingLogger.messages should not be empty
    all(recordingLogger.messages) should not include BearerToken
  }

  "MdgConnector" should {

    "send the serialised DEC64 XML with XML content type, bearer token and X-Correlation-ID headers" in {
      setupMdgDec64ToReturn(204)

      await(sendValidRequest()) shouldBe MdgRequestSuccessful

      val request = theRequestMadeToMdgDec64()
      request.getHeader("Content-Type") should startWith("application/xml")
      request.getHeader("Accept") shouldBe "application/xml"
      request.getHeader("Authorization") shouldBe s"Bearer $BearerToken"
      request.getHeader("X-Correlation-ID") should fullyMatch regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
      request.getBodyAsString shouldBe serialiser.serialise(TransmissionRequestOne)
    }

    "generate a new X-Correlation-ID for every request" in {
      setupMdgDec64ToReturn(204)

      await(sendValidRequest())
      await(sendValidRequest())

      import com.github.tomakehurst.wiremock.client.WireMock.{findAll, postRequestedFor, urlEqualTo}
      import scala.jdk.CollectionConverters.*
      val ids = findAll(postRequestedFor(urlEqualTo(MdgDec64Context))).asScala.map(_.getHeader("X-Correlation-ID"))
      ids should have size 2
      ids.distinct should have size 2
    }

    Seq(200, 202, 204).foreach { status =>
      s"treat $status as success" in {
        setupMdgDec64ToReturn(status)

        await(sendValidRequest()) shouldBe MdgRequestSuccessful
        tokenWasNeverLogged()
      }
    }

    Seq(400, 401, 403, 404, 409, 422).foreach { status =>
      s"treat $status as a permanent failure (no retry)" in {
        setupMdgDec64ToReturn(status, body = "<error>bad</error>")

        val result = await(sendValidRequest())

        result shouldBe a[MdgRequestFatalError]
        result.error.get should include(s"returned status $status")
        result.error.get should include("<error>bad</error>")
        tokenWasNeverLogged()
      }
    }

    Seq(500, 502, 503, 504).foreach { status =>
      s"treat $status as retryable" in {
        setupMdgDec64ToReturn(status, body = "unavailable")

        val result = await(sendValidRequest())

        result shouldBe a[MdgRequestError]
        result.error.get should include(s"returned status $status")
        tokenWasNeverLogged()
      }
    }

    "treat a timeout as retryable" in {
      setupMdgDec64ToDelay(204, delayMillis = RequestTimeoutMillis * 3)

      await(sendValidRequest())(using defaultTimeout) shouldBe a[MdgRequestError]
      tokenWasNeverLogged()
    }

    "treat a connection reset as retryable" in {
      setupMdgDec64ToFault(Fault.CONNECTION_RESET_BY_PEER)

      await(sendValidRequest()) shouldBe a[MdgRequestError]
      tokenWasNeverLogged()
    }

    "treat an empty response as retryable" in {
      setupMdgDec64ToFault(Fault.EMPTY_RESPONSE)

      await(sendValidRequest()) shouldBe a[MdgRequestError]
      tokenWasNeverLogged()
    }

    "treat a connection refused as retryable" in {
      stopMockServer()
      try {
        await(sendValidRequest()) shouldBe a[MdgRequestError]
        tokenWasNeverLogged()
      } finally startMockServer()
    }

    "log the request at debug level without the bearer token" in {
      setupMdgDec64ToReturn(204)

      await(sendValidRequest())

      val debugLine = recordingLogger.messages.find(_.startsWith("Sent request to MDG")).get
      debugLine should include("X-Correlation-ID")
      debugLine should not include "Authorization"
      tokenWasNeverLogged()
    }
  }
}
