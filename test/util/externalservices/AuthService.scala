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

package util.externalservices

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, PrivilegedApplication}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.customs.declaration.model.Eori
import util.TestData

trait AuthService {

  val authUrl = "/auth/authorise"
  private val authUrlMatcher = urlEqualTo(authUrl)

  private val customsEnrolmentName = "HMRC-CUS-ORG"

  private val cspAuthorisationPredicate = Enrolment("write:customs-declaration") and AuthProviders(PrivilegedApplication)
  private val nonCspAuthorisationPredicate = Enrolment(customsEnrolmentName) and AuthProviders(GovernmentGateway)
  private val nonCspRetrieval = Retrievals.authorisedEnrolments

  private def bearerTokenMatcher(bearerToken: String)= equalTo("Bearer " + bearerToken)

  private def authRequestJson(predicate: Predicate, retrievals: Retrieval[_]*): String = {
    val predicateJsArray = predicate.toJson match {
      case arr: JsArray => arr
      case other => Json.arr(other)
    }
    val js =
      s"""
         |{
         |  "authorise": $predicateJsArray,
         |  "retrieve": [${retrievals.flatMap(_.propertyNames).map(Json.toJson(_)).mkString(",")}]
         |}
    """.stripMargin
    js
  }

  def authServiceAuthorizesCSP(bearerToken: String = TestData.cspBearerToken): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(cspAuthorisationPredicate)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody("{}")
      )
    )
  }

  def authServiceUnauthorisesScopeForCSP(bearerToken: String = TestData.cspBearerToken): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(cspAuthorisationPredicate)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.UNAUTHORIZED)
          .withHeader(WWW_AUTHENTICATE, """MDTP detail="InsufficientEnrolments"""")
      )
    )
  }

  def authServiceAuthorizesNonCspWithEori(bearerToken: String = TestData.nonCspBearerToken,
                                          eori: Eori = TestData.declarantEori): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(nonCspAuthorisationPredicate, nonCspRetrieval)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(
            s"""
              |{
              |  "authorisedEnrolments": [ ${enrolmentRetrievalJson(customsEnrolmentName, "EORINumber", eori.value)} ]
              |}
            """.stripMargin
          )
      )
    )
  }

  def authServiceUnauthorisesCustomsEnrolmentForNonCSP(bearerToken: String = TestData.nonCspBearerToken): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(nonCspAuthorisationPredicate, nonCspRetrieval)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
      .willReturn(
        aResponse()
          .withStatus(Status.UNAUTHORIZED)
          .withHeader(WWW_AUTHENTICATE, """MDTP detail="InsufficientEnrolments"""")
      )
    )
  }

  def verifyAuthServiceCalledForCsp(bearerToken: String = TestData.cspBearerToken): Unit = {
    verify(1, postRequestedFor(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(cspAuthorisationPredicate)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
    )
  }

  def verifyAuthServiceCalledForNonCsp(bearerToken: String = TestData.nonCspBearerToken): Unit = {
    verify(1, postRequestedFor(authUrlMatcher)
      .withRequestBody(equalToJson(authRequestJson(nonCspAuthorisationPredicate, nonCspRetrieval)))
      .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
    )
  }

  private def enrolmentRetrievalJson(enrolmentKey: String,
                                     identifierName: String,
                                     identifierValue: String): String = {
    s"""
       |{
       | "key": "$enrolmentKey",
       | "identifiers": [
       |   {
       |     "key": "$identifierName",
       |     "value": "$identifierValue"
       |   }
       | ]
       |}
    """.stripMargin
  }

}
