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
import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders}
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import util.WireMockRunner
import java.util

import scala.collection.JavaConverters._

trait CustomsNotificationService extends WireMockRunner {

  private val notifyPath = urlMatching("/customs-notification/notify")

  def registrationServiceIsRunning() {
    stubFor(post(notifyPath).
      willReturn(
        aResponse()
          .withStatus(ACCEPTED)))
  }

  def aRequestWasMadeToRegistrationService(): Tuple2[Map[String, String],String] =  {
    verify(1, postRequestedFor(notifyPath))
    val req = findAll(postRequestedFor(notifyPath)).get(0)
    val keys: List[String] = List.concat(req.getHeaders().keys().asScala)
    (Map(keys map {s => (s, req.getHeader(s))} : _*),req.getBodyAsString)
  }
}
