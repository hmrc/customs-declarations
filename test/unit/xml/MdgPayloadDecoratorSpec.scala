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

package unit.xml

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.model.SubscriptionFieldsId
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.play.test.UnitSpec
import util.TestData
import util.TestData.TestCspValidatedPayloadRequest

import scala.xml.NodeSeq

class MdgPayloadDecoratorSpec extends UnitSpec with MockitoSugar {

  private val xml: NodeSeq = <node1></node1>

  private val clientId = SubscriptionFieldsId("clientId")

  private val year = 2017
  private val monthOfYear = 6
  private val dayOfMonth = 8
  private val hourOfDay = 13
  private val minuteOfHour = 55
  private val secondOfMinute = 0
  private val millisOfSecond = 0
  private val dateTime = new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond, DateTimeZone.UTC)
  private val payloadWrapper = new MdgPayloadDecorator()

  private implicit val vpr = TestCspValidatedPayloadRequest

  private def wrapPayloadWithBadgeIdentifier() = payloadWrapper.wrap(xml, clientId, dateTime)
  private def wrapPayloadWithOutBadgeIdentifier() = payloadWrapper.wrap(xml, clientId, dateTime)


  "WcoDmsPayloadWrapper " should {
    "wrap passed XML in DMS wrapper" in {
      val result = wrapPayloadWithBadgeIdentifier()

      val reqDet = result \\ "requestDetail"
      reqDet.head.child.contains(<node1 />) shouldBe true
    }

    "set the receipt date in the wrapper" in {
      val result = wrapPayloadWithBadgeIdentifier()

      val rd = result \\ "receiptDate"

      rd.head.text shouldBe "2017-06-08T13:55:00Z"
    }

    "set the conversationId" in {
      val result = wrapPayloadWithBadgeIdentifier()

      val rd = result \\ "conversationID"

      rd.head.text shouldBe TestData.conversationId.toString
    }

    "set the clientId" in {
      val result = wrapPayloadWithBadgeIdentifier()

      val rd = result \\ "clientID"

      rd.head.text shouldBe clientId.value
    }

    "set the badgeIdentifier when present" in {
      val result = wrapPayloadWithBadgeIdentifier()

      val rd = result \\ "badgeIdentifier"

      rd.head.text shouldBe TestData.badgeIdentifier.value
    }

    "should not set the badgeIdentifier when absent" in {
      val result = wrapPayloadWithOutBadgeIdentifier()

      val rd = result \\ "badgeIdentifier"

      rd.isEmpty
    }

  }

}
