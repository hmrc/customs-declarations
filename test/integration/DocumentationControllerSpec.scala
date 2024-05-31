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

package integration

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.io.FileNotFoundException
import scala.concurrent.Future

class DocumentationControllerSpec extends IntegrationTestSpec with MockitoSugar with GuiceOneAppPerSuite {

  private implicit lazy val materializer = app.materializer

  private val definitionJsonContent = getResourceFileContent("/public/api/definition.json")
  private val applicationRamlContent = getResourceFileContent("/public/api/conf/1.0/application.raml")

  override implicit lazy val app: Application = GuiceApplicationBuilder(
    modules = Seq()).
    configure(
      Map(
        "play.http.router" -> "definition.Routes",
        "application.logger.name" -> "customs-api-common",
        "appName" -> "customs-declarations",
        "appUrl" -> "http://customs-wco-declaration.service",
        "auditing.enabled" -> false,
        "auditing.traceRequests" -> false
      )
    ).build()

  "DocumentationController" should {
    "serve definition.json" in assertRoutedContent("/api/definition", definitionJsonContent)

    "serve application.raml" in assertRoutedContent("/api/conf/1.0/application.raml", applicationRamlContent)
  }

  private def assertRoutedContent(uri: String, expectedContent: String) = {

    val result: Option[Future[Result]] = route(app, FakeRequest("GET", uri))

    result shouldBe Symbol("defined")
    val resultFuture: Future[Result] = result.get

    status(resultFuture) shouldBe OK
    contentAsString(resultFuture) shouldBe expectedContent
  }

  private def getResourceFileContent(resourceFile: String): String = {
    val is = Option(getClass.getResourceAsStream(resourceFile)).getOrElse(
      throw new FileNotFoundException(s"Resource file not found: $resourceFile"))
    scala.io.Source.fromInputStream(is).mkString
  }
}
