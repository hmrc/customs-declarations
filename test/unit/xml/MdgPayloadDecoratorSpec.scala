/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import util.ApiSubscriptionFieldsTestData.apiSubscriptionFieldsResponse
import util.TestData._

import java.time.{LocalDateTime, ZoneOffset}
import scala.xml.NodeSeq

class MdgPayloadDecoratorSpec extends AnyWordSpecLike with MockitoSugar with Matchers {

  private val xml: NodeSeq = <node1></node1>

  private val year = 2017
  private val monthOfYear = 6
  private val dayOfMonth = 8
  private val hourOfDay = 13
  private val minuteOfHour = 55
  private val secondOfMinute = 0
  private val millisOfSecond = 0
  private val dateTime = LocalDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond).toInstant(ZoneOffset.UTC)

  private val payloadWrapper = new MdgPayloadDecorator()

  "MdgPayloadDecorator for Csp with Eori " should {
    implicit val implicitVpr = TestCspWithEoriNoBadgeIdValidatedPayloadRequest
    def wrapPayload(): NodeSeq = payloadWrapper.wrap(xml, apiSubscriptionFieldsResponse, dateTime)

    "omit the badgeIdentifier" in {
      val result = wrapPayload()

      val rd = result \\ "badgeIdentifier"

      rd should be (empty)
    }

    "set the originatingPartyID as Eori" in {
      val result = wrapPayload()

      val rd = result \\ "originatingPartyID"

      rd.head.text shouldBe "ZZ123456789000"
    }

    "set the authenticatedPartyID when present" in {
      val result = wrapPayload()

      val rd = result \\ "authenticatedPartyID"
      rd.head.text shouldBe "ZZ123456789000"
    }
  }

  "MdgPayloadDecorator for Csp with BadgeIdentifier" should {
    implicit val implicitVpr = TestCspWithBadgeIdNoEoriValidatedPayloadRequest
    def wrapPayload(): NodeSeq = payloadWrapper.wrap(xml, apiSubscriptionFieldsResponse, dateTime)

    "wrap passed XML in DMS wrapper" in {
      val result = wrapPayload()

      val reqDet = result \\ "requestDetail"
      reqDet.head.child.contains(<node1/>) shouldBe true
    }

    "set the receipt date in the wrapper" in {
      val result = wrapPayload()

      val rd = result \\ "receiptDate"

      rd.head.text shouldBe "2017-06-08T13:55:00Z"
    }

    "set the conversationId" in {
      val result = wrapPayload()

      val rd = result \\ "conversationID"

      rd.head.text shouldBe conversationId.toString
    }

    "set the clientId" in {
      val result = wrapPayload()

      val rd = result \\ "clientID"

      rd.head.text shouldBe apiSubscriptionFieldsResponse.fieldsId.toString
    }

    "set the badgeIdentifier correctly" in {
      val result = wrapPayload()

      val rd = result \\ "badgeIdentifier"
      rd.head.text shouldBe "BADGEID123"
    }

    "set the originatingPartyID as badgeId" in {
      val result = wrapPayload()

      val rd = result \\ "originatingPartyID"
      rd.head.text shouldBe "BADGEID123"
    }

    "set the authenticatedPartyID when present" in {
      val result = wrapPayload()

      val rd = result \\ "authenticatedPartyID"
      rd.head.text shouldBe "ZZ123456789000"
    }
  }

  "MdgPayloadDecorator for Non Csp" should {
    implicit val implicitVpr = TestNonCspValidatedPayloadRequest
    def wrapPayloadWithoutBadgeIdentifier(): NodeSeq = payloadWrapper.wrap(xml, apiSubscriptionFieldsResponse, dateTime)

    "wrap passed XML in DMS wrapper" in {
      val result = wrapPayloadWithoutBadgeIdentifier()

      val reqDet = result \\ "requestDetail"
      reqDet.head.child.contains(<node1/>) shouldBe true
    }

    "set the receipt date in the wrapper" in {
      val result = wrapPayloadWithoutBadgeIdentifier()

      val rd = result \\ "receiptDate"

      rd.head.text shouldBe "2017-06-08T13:55:00Z"
    }

    "set the conversationId" in {
      val result = wrapPayloadWithoutBadgeIdentifier()

      val rd = result \\ "conversationID"

      rd.head.text shouldBe conversationId.toString
    }

    "set the clientId" in {
      val result = wrapPayloadWithoutBadgeIdentifier()

      val rd = result \\ "clientID"

      rd.head.text shouldBe apiSubscriptionFieldsResponse.fieldsId.toString
    }

    "should not set the badgeIdentifier when absent" in {
      val result = wrapPayloadWithoutBadgeIdentifier()

      val rd = result \\ "badgeIdentifier"

      rd.isEmpty shouldBe true
    }

    "set the authenticatedPartyID when present" in {
      val result = wrapPayloadWithoutBadgeIdentifier()

      val rd = result \\ "authenticatedPartyID"
      rd.head.text shouldBe apiSubscriptionFieldsResponse.fields.authenticatedEori.get
    }

    "NOT set the originatingPartyID" in {
      val result = wrapPayloadWithoutBadgeIdentifier()

      val rd: NodeSeq = result \\ "originatingPartyID"
      rd shouldBe NodeSeq.Empty
    }

  }

}
