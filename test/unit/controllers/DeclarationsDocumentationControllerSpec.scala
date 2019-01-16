/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.declaration.controllers.DeclarationsDocumentationController
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger

class DeclarationsDocumentationControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfterEach {

  private val mockService = mock[HttpErrorHandler]

  private val mockLogger = mock[DeclarationsLogger]

  private val v1WhitelistedAppIdsConfigs = Map(
    "api.access.version-1.0.whitelistedApplicationIds.0" -> "v1AppId-1",
    "api.access.version-1.0.whitelistedApplicationIds.1" -> "v1AppId-2")

  private val v2WhitelistedAppIdsConfigs = Map(
    "api.access.version-2.0.whitelistedApplicationIds.0" -> "v2AppId-1",
    "api.access.version-2.0.whitelistedApplicationIds.1" -> "v2AppId-2")


  private val v3WhitelistedAppIdsConfigs = Map(
    "api.access.version-3.0.whitelistedApplicationIds.0" -> "v3AppId-1",
    "api.access.version-3.0.whitelistedApplicationIds.1" -> "v3AppId-2")

  private def getApiDefinitionWith(configMap: Map[String, Any]) =
    new DeclarationsDocumentationController(mockService, play.api.Configuration.from(configMap), mockLogger)
      .definition()

  override def beforeEach() {
    reset(mockService)
  }

  "API Definition" should {

    "be correct when V1 & V2 are PUBLIC by default and V3 is always private" in {
      val result = getApiDefinitionWith(Map())(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(None, None, None)
    }

    "be correct when V1 is PRIVATE & V2 is public  and V3 is always private" in {
      val result = getApiDefinitionWith(v1WhitelistedAppIdsConfigs)(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(expectedV1WhitelistedAppIds = Some(v1WhitelistedAppIdsConfigs.values), None, None)
    }

    "be correct when V1 is PUBLIC & V2 is PRIVATE  and V3 is always private" in {
      val result = getApiDefinitionWith(v2WhitelistedAppIdsConfigs)(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(expectedV1WhitelistedAppIds = None, expectedV2WhitelistedAppIds = Some(v2WhitelistedAppIdsConfigs.values), None)
    }

    "be correct when V1 & V2 & V3 are PRIVATE" in {
      val result = getApiDefinitionWith(v1WhitelistedAppIdsConfigs ++ v2WhitelistedAppIdsConfigs ++ v3WhitelistedAppIdsConfigs)(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson(
        expectedV1WhitelistedAppIds = Some(v1WhitelistedAppIdsConfigs.values),
        expectedV2WhitelistedAppIds = Some(v2WhitelistedAppIdsConfigs.values),
        expectedV3WhitelistedAppIds = Some(v3WhitelistedAppIdsConfigs.values))
    }
  }

  private def expectedJson(expectedV1WhitelistedAppIds: Option[Iterable[String]],
                           expectedV2WhitelistedAppIds: Option[Iterable[String]],
                           expectedV3WhitelistedAppIds: Option[Iterable[String]]
                          ) =
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
         |               """.stripMargin
        +

        expectedV1WhitelistedAppIds.fold(""" "type":"PUBLIC" """)(ids =>
          """ "type":"PRIVATE", "whitelistedApplicationIds":[ """.stripMargin
            + ids.map(x => s""" "$x" """).mkString(",") + "]"
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
           |               },
           |               {
           |                  "name": "authenticatedEori",
           |                  "description": "What's your Economic Operator Registration and Identification (EORI) number?",
           |                  "type": "STRING",
           |                  "hint": "This is your EORI that will associate your application with you as a CSP"
           |               }
           |            ]
           |         },
           |         {
           |            "version":"2.0",
           |            "status":"BETA",
           |            "endpointsEnabled":true,
           |            "access":{""".stripMargin
        +
        expectedV2WhitelistedAppIds.fold(""" "type":"PUBLIC" """)(ids =>
          """ "type":"PRIVATE", "whitelistedApplicationIds":[ """.stripMargin
            + ids.map(x => s""" "$x" """).mkString(",") + "]"
        ) +
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
           |               },
           |               {
           |                  "name": "authenticatedEori",
           |                  "description": "What's your Economic Operator Registration and Identification (EORI) number?",
           |                  "type": "STRING",
           |                  "hint": "This is your EORI that will associate your application with you as a CSP"
           |               }
           |            ]
           |         },
           |         {
           |            "version":"3.0",
           |            "status":"BETA",
           |            "endpointsEnabled":true,
           |            "access":{
           |                "type":"PRIVATE",
           |                "whitelistedApplicationIds":""".stripMargin + expectedV3WhitelistedAppIds.fold("[]")(ids =>
        """ [ """.stripMargin + ids.map(x => s""" "$x" """).mkString(",") + "]") + """
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
           |               },
           |               {
           |                  "name": "authenticatedEori",
           |                  "description": "What's your Economic Operator Registration and Identification (EORI) number?",
           |                  "type": "STRING",
           |                  "hint": "This is your EORI that will associate your application with you as a CSP"
           |               }
           |            ]
           |         }
           |      ]
           |   }
           |}
      """.stripMargin)

}
