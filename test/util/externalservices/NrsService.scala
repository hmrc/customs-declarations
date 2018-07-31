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

trait NrsService {

  val nrsUrl = "/submission"
  private val nrsUrlMatcher = urlEqualTo(nrsUrl)

  def verifyNrsServiceCalled(): Unit = {
    verify(1, postRequestedFor(nrsUrlMatcher)
      // Body has been verified as part of unit test
      .withHeader("Content-Type", equalTo("application/json"))
      .withHeader("X-API-KEY", equalTo("nrs-api-key"))
    )
  }
}
