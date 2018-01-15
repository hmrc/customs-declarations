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

package unit.logging

import uk.gov.hmrc.customs.declaration.logging.LoggingHelper
import uk.gov.hmrc.customs.declaration.model.{FieldsId, Ids}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import util.RequestHeaders._
import util.{ApiSubscriptionFieldsTestData, TestData}

class LoggingHelperSpec extends UnitSpec {

  private val fromHeader = ApiSubscriptionFieldsTestData.fieldsIdString
  private val notFromHeader = "FIELDS_ID_NOT_FROM_HEADER"
  private val fieldsIdNotFromHeader = FieldsId(notFromHeader)
  private val theIds = Ids(TestData.conversationId, fieldsIdNotFromHeader)
  private val allHeaders = Seq(AUTH_HEADER, X_CLIENT_ID_HEADER, API_SUBSCRIPTION_FIELDS_ID_HEADER, X_CONVERSATION_ID_HEADER)
  private val justXClientIdHeader = Seq(X_CLIENT_ID_HEADER)
  private val errorMsg = "ERROR"
  private val warnMsg = "WARN"
  private val infoMsg = "INFO"
  private val debugMsg = "DEBUG"

  private val hcWithAllHeaders = HeaderCarrier(extraHeaders = allHeaders)

  "LoggingHelper with no ids and no logging related headers present" should {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    "format ERROR" in {
      LoggingHelper.formatError(errorMsg) shouldBe errorMsg
    }

    "format WARN"  in {
      LoggingHelper.formatWarn(warnMsg) shouldBe warnMsg
    }

    "format INFO" in {
      LoggingHelper.formatInfo(infoMsg) shouldBe infoMsg
    }

    "format DEBUG" in {
      LoggingHelper.formatDebug(debugMsg) shouldBe s"$debugMsg\nrequest headers=${hc.headers}"
    }
  }

  "LoggingHelper with no ids" should {
    implicit val hc: HeaderCarrier = hcWithAllHeaders

    "format ERROR" in {
      LoggingHelper.formatError(errorMsg) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$fromHeader] " + errorMsg
    }

    "format WARN"  in {
      LoggingHelper.formatWarn(warnMsg) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$fromHeader] " + warnMsg
    }

    "format INFO" in {
      LoggingHelper.formatInfo(infoMsg) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$fromHeader] " + infoMsg
    }

    "format DEBUG" in {
      LoggingHelper.formatDebug(debugMsg) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$fromHeader] $debugMsg\nrequest headers=${hcWithAllHeaders.headers}"
    }
  }

  "LoggingHelper with ids" should {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = justXClientIdHeader)

    "format INFO" in {
      LoggingHelper.formatInfo(infoMsg, Some(theIds)) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$notFromHeader][conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d] " + infoMsg
    }
  }

  "LoggingHelper with fieldsId in both header and ids" should {
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = allHeaders)

    "format INFO" in {
      LoggingHelper.formatInfo(infoMsg, Some(theIds)) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$fromHeader][conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d] " + infoMsg
    }
  }

  "LoggingHelper DEBUG" should {
    implicit val hc: HeaderCarrier = hcWithAllHeaders

    "format with payload" in {
      LoggingHelper.formatDebug(debugMsg, maybePayload = Some("PAYLOAD")) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$fromHeader] $debugMsg\nrequest headers=${hcWithAllHeaders.headers}\npayload=PAYLOAD"
    }
  }

  "LoggingHelper" should {
    "format INFO with headers" in {
      LoggingHelper.formatInfo(infoMsg, allHeaders) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$fromHeader] $infoMsg"
    }

    "format DEBUG with headers" in {
      LoggingHelper.formatDebug(debugMsg, allHeaders) shouldBe s"[clientId=SOME_X_CLIENT_ID][fieldsId=$fromHeader] $debugMsg\nrequest headers=$allHeaders"
    }

  }
}
