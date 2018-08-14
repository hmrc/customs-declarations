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

import org.scalatest.mockito.MockitoSugar
import play.api.mvc.AnyContentAsXml
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model.GoogleAnalyticsValues
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{AnalyticsValuesAndConversationIdRequest, AuthorisedRequest}
import uk.gov.hmrc.play.test.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData._

class DeclarationsLoggerSpec extends UnitSpec with MockitoSugar {

  trait SetUp {
    val mockCdsLogger: CdsLogger = mock[CdsLogger]
    val logger = new DeclarationsLogger(mockCdsLogger)
    implicit val implicitVpr: AuthorisedRequest[AnyContentAsXml] = AnalyticsValuesAndConversationIdRequest(conversationId, GoogleAnalyticsValues.Submit, FakeRequest()
      .withXmlBody(TestXmlPayload).withHeaders("Content-Type" -> "Some-Content-Type"))
      .toValidatedHeadersRequest(TestExtractedHeaders)
      .toCspAuthorisedRequest(badgeIdentifier, Some(nrsRetrievalValues))
  }

  //private val nrsExpectedString= "Some(NrsRetrievalData(Some(internalId),Some(externalId),Some(agentCode),Credentials(providerId,providerType),500,Some(ninov),Some(saUtr),Name(Some(name),Some(lastname)),Some(1993-08-14),Some(nrsEmailValue),AgentInformation(Some(agentId),Some(agentCode),Some(agentFriendlyName)),Some(groupIdentifierValue),Some(User),Some(MdtpInformation(deviceId,sessionId)),ItmpName(Some(givenName),Some(middleName),Some(familyName)),Some(1993-08-14),ItmpAddress(Some(line1),Some(line2),Some(line3),Some(line4),Some(line5),Some(postCode),Some(countryName),Some(countryCode)),Some(Individual),Some(STRONG),LoginTimes(2018-07-01T12:00:00.000+01:00,Some(2018-07-01T18:00:00.000+01:00))))"

  "DeclarationsLogger" should {
    "debug(s: => String)" in new SetUp {
      logger.debug("msg")

      PassByNameVerifier(mockCdsLogger, "debug")
                        //[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=CSP(BADGEID123)] msg
                        //[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=CSP(BADGEID123)] msg
                        //[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=Csp$(BADGEID123)] msg
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=Csp(BADGEID123)] msg")
        .verify()
    }
    "debug(s: => String, e: => Throwable)" in new SetUp {
      logger.debug("msg", emulatedServiceFailure)

      PassByNameVerifier(mockCdsLogger, "debug")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=Csp(BADGEID123)] msg")
        .withByNameParam(emulatedServiceFailure)
        .verify()
    }
    "debugFull(s: => String)" in new SetUp {
      logger.debugFull("msg")

      PassByNameVerifier(mockCdsLogger, "debug")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d] msg headers=Map(Content-Type -> Some-Content-Type)")
        .verify()
    }
    "info(s: => String)" in new SetUp {
      logger.info("msg")

      PassByNameVerifier(mockCdsLogger, "info")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=Csp(BADGEID123)] msg")
        .verify()
    }
    "warn(s: => String)" in new SetUp {
      logger.warn("msg")

      PassByNameVerifier(mockCdsLogger, "warn")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=Csp(BADGEID123)] msg")
        .verify()
    }
    "error(s: => String, e: => Throwable)" in new SetUp {
      logger.error("msg", emulatedServiceFailure)

      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=Csp(BADGEID123)] msg")
        .withByNameParam(emulatedServiceFailure)
        .verify()
    }
    "error(s: => String)" in new SetUp {
      logger.error("msg")

      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam("[conversationId=38400000-8cf0-11bd-b23e-10b96e4ef00d][clientId=SOME_X_CLIENT_ID][requestedApiVersion=1.0][authorisedAs=Csp(BADGEID123)] msg")
        .verify()
    }
    "errorWithoutRequestContext(s: => String)" in new SetUp {
      logger.errorWithoutRequestContext("msg")

      PassByNameVerifier(mockCdsLogger, "error")
        .withByNameParam("msg")
        .verify()
    }
  }
}
