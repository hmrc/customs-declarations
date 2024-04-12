/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.logging

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.logging.CdsLogger
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.UnitSpec

import java.io.{ByteArrayOutputStream, PrintStream}
import scala.io.Source

class CdsLoggerSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    private val mockServiceConfig = mock[ServicesConfig]
    lazy val cdsLogger = new CdsLogger(mockServiceConfig)

    val cdsLoggerName = "customs-notification-gateway"

    when(mockServiceConfig.getString(any[String])).thenReturn(cdsLoggerName)

    protected def captureLogging(blockWithLogging: => Any): List[String] = {
      import ch.qos.logback.classic.{BasicConfigurator, LoggerContext}
      import org.slf4j.LoggerFactory

      LoggerFactory.getILoggerFactory match {
        case lc: LoggerContext =>
          lc.reset()
          new BasicConfigurator().configure(lc)

        case unsupported => fail(s"Unexpected LoggerFactory configured in SLF4J: $unsupported")
      }

      val baos = new ByteArrayOutputStream()
      val stdout = System.out
      System.setOut(new PrintStream(baos))

      try blockWithLogging
      finally System.setOut(stdout)
      Source.fromBytes(baos.toByteArray).getLines().toList
    }
  }

  "CdsLogger" should {
    "log debug" in new Setup {
      val msg = "debug"

      val output: List[String] = captureLogging {
        cdsLogger.debug(msg)
      }

      output should have size 1
      val loggedMessage :: Nil = output
      loggedMessage should endWith(s"DEBUG $cdsLoggerName -- $msg")
    }

    "log debug with exception" in new Setup {
      val msg = "debug"
      val exception = new RuntimeException("debug")

      val output: List[String] = captureLogging {
        cdsLogger.debug(msg, exception)
      }

      output.size should be > 2
      val loggedMessage :: exceptionMessage :: stacktrace = output
      loggedMessage should endWith(s"DEBUG $cdsLoggerName -- $msg")
      exceptionMessage shouldBe exception.toString
      stacktrace foreach(_ should startWith("\tat "))
    }

    "log info" in new Setup {
      val msg = "info"

      val output: List[String] = captureLogging {
        cdsLogger.info(msg)
      }

      output should have size 1
      val loggedMessage :: Nil = output
      loggedMessage should endWith(s"INFO $cdsLoggerName -- $msg")
    }

    "log info with exception" in new Setup {
      val msg = "info"
      val exception = new RuntimeException("info")

      val output: List[String] = captureLogging {
        cdsLogger.info(msg, exception)
      }

      output.size should be > 2
      val loggedMessage :: exceptionMessage :: stacktrace = output
      loggedMessage should endWith(s"INFO $cdsLoggerName -- $msg")
      exceptionMessage shouldBe exception.toString
      stacktrace foreach(_ should startWith("\tat "))
    }

    "log warn" in new Setup {
      val msg = "warn"

      val output: List[String] = captureLogging {
        cdsLogger.warn(msg)
      }

      output should have size 1
      val loggedMessage :: Nil = output
      loggedMessage should endWith(s"WARN $cdsLoggerName -- $msg")

    }

    "log warn with exception" in new Setup {
      val msg = "warn"
      val exception = new RuntimeException("warn")

      val output: List[String] = captureLogging {
        cdsLogger.warn(msg, exception)
      }

      output.size should be > 2
      val loggedMessage :: exceptionMessage :: stacktrace = output
      loggedMessage should endWith(s"WARN $cdsLoggerName -- $msg")
      exceptionMessage shouldBe exception.toString
      stacktrace foreach(_ should startWith("\tat "))
    }

    "log error" in new Setup {
      val msg = "error"

      val output: List[String] = captureLogging {
        cdsLogger.error(msg)
      }

      output should have size 1
      val loggedMessage :: Nil = output
      loggedMessage should endWith(s"ERROR $cdsLoggerName -- $msg")
    }

    "log error with exception" in new Setup {
      val msg = "error"
      val exception = new RuntimeException("error")

      val output: List[String] = captureLogging {
        cdsLogger.error(msg, exception)
      }

      output.size should be > 2
      val loggedMessage :: exceptionMessage :: stacktrace = output
      loggedMessage should endWith(s"ERROR $cdsLoggerName -- $msg")
      exceptionMessage shouldBe exception.toString
      stacktrace foreach(_ should startWith("\tat "))
    }
  }
}
