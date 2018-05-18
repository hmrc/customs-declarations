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

package unit.controllers

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import uk.gov.hmrc.customs.declaration.controllers.DeclarationsDocumentationController

class DeclarationsDocumentationControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfterEach {

  private val mockService = mock[HttpErrorHandler]

  private def getApiDefinitionWith(configMap: Map[String, Any]) =
    new DeclarationsDocumentationController(mockService, play.api.Configuration.from(configMap))
      .definition()

  override def beforeEach() {
    reset(mockService)
  }

  "DocumentationController" should {

    "return definition with V1 & V2 PUBLIC by default" in {
      val result = getApiDefinitionWith(Map())(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson()
    }

    "return correct definition when V1 is PRIVATE" in {
      val result = getApiDefinitionWith(Map("api.access.version-1.0.private" -> true))(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(v1AccessType = "PRIVATE", v1WhitelistedAppIds = Some(Seq()))
    }

    "return correct definition when V1 is PRIVATE and whitelisted Application Ids are given" in {
      val result = getApiDefinitionWith(Map(("api.access.version-1.0.private" -> true),
        ("api.access.version-1.0.whitelistedApplicationIds.0" -> "v1AppId-1"),
        ("api.access.version-1.0.whitelistedApplicationIds.1" -> "v1AppId-2")))(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(v1AccessType = "PRIVATE", v1WhitelistedAppIds = Some(Seq("v1AppId-1", "v1AppId-2")))
    }

    "return correct definition when V2 is PRIVATE" in {
      val result = getApiDefinitionWith(Map("api.access.version-2.0.private" -> true))(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(v2AccessType = "PRIVATE", v2WhitelistedAppIds = Some(Seq()))
    }

    "return correct definition when V2 is PRIVATE and whitelisted Application Ids are given" in {
      val result = getApiDefinitionWith(Map(("api.access.version-2.0.private" -> true),
        ("api.access.version-2.0.whitelistedApplicationIds.0" -> "v2AppId-1"),
        ("api.access.version-2.0.whitelistedApplicationIds.1" -> "v2AppId-2")))(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(v2AccessType = "PRIVATE", v2WhitelistedAppIds = Some(Seq("v2AppId-1", "v2AppId-2")))
    }


  }

  private def expectedJson(v1AccessType: String = "PUBLIC",
                           v1WhitelistedAppIds: Option[Seq[String]] = None,
                           v2AccessType: String = "PUBLIC",
                           v2WhitelistedAppIds: Option[Seq[String]] = None) =
    Json.parse(
      s"""
         |{
         |   "scopes":[
         |      {
         |         "key":"write:customs-declaration",
         |         "name":"Submit a Customs Declaration",
         |         "description":"Submit a Customs Declaration"
         |      }
         |   ],
         |   "api":{
         |      "name":"Customs Declarations",
         |      "description":"Single WCO-compliant Customs Declarations API",
         |      "context":"customs/declarations",
         |      "versions":[
         |         {
         |            "version":"1.0",
         |            "status":"BETA",
         |            "endpointsEnabled":true,
         |            "access":{
         |               "type":"$v1AccessType"
         |               """.stripMargin
        +

        v1WhitelistedAppIds.fold("")(ids =>
          """, "whitelistedApplicationIds":[ """ + ids.map(x => s""" "$x" """).mkString(",") + "]"
        )

        +
        s"""
           |            },
           |            "fieldDefinitions":[
           |               {
           |                  "name":"callbackUrl",
           |                  "description":"What's your callback URL for declaration submissions?",
           |                  "type":"URL",
           |                  "hint":"This is how we'll notify you when we've processed them. It must include https and port 443"
           |               },
           |               {
           |                  "name":"securityToken",
           |                  "description":"What's the value of the HTTP Authorization header we should use to notify you?",
           |                  "type":"SecureToken",
           |                  "hint":"For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk"
           |               }
           |            ]
           |         },
           |         {
           |            "version":"2.0",
           |            "status":"BETA",
           |            "endpointsEnabled":true,
           |            "access":{
           |               "type":"$v2AccessType" """.stripMargin
        +
        v2WhitelistedAppIds.fold("")(ids =>
          """, "whitelistedApplicationIds":[ """ + ids.map(x => s""" "$x" """).mkString(",") + "]")
        +
        s"""
           |            },
           |            "fieldDefinitions":[
           |               {
           |                  "name":"callbackUrl",
           |                  "description":"What's your callback URL for declaration submissions?",
           |                  "type":"URL",
           |                  "hint":"This is how we'll notify you when we've processed them. It must include https and port 443"
           |               },
           |               {
           |                  "name":"securityToken",
           |                  "description":"What's the value of the HTTP Authorization header we should use to notify you?",
           |                  "type":"SecureToken",
           |                  "hint":"For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk"
           |               }
           |            ]
           |         }
           |      ]
           |   }
           |}
      """.stripMargin)

}
