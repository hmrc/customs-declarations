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

package unit.controllers

import controllers.Assets
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.customs.declaration.controllers.DeclarationsDocumentationController
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger

class DeclarationsDocumentationControllerSpec extends PlaySpec with MockitoSugar with Results with BeforeAndAfterEach {

  private val mockLogger = mock[DeclarationsLogger]

  private def getApiDefinitionWith(configMap: Map[String, Any]) =
    new DeclarationsDocumentationController(mock[Assets], Helpers.stubControllerComponents(), play.api.Configuration.from(configMap), mockLogger)
      .definition()

  "API Definition" should {

    "be correct when V1, V2 are private" in {
      val result = getApiDefinitionWith(Map())(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson()
    }
  }

  private def expectedJson(): Any =
    Json.parse(
      """
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
         |              "type": "PRIVATE"
         |            },
         |            "fieldDefinitions":[
         |               {
         |                  "name":"callbackUrl",
         |                  "description":"What's your callback URL for declaration submissions?",
         |                  "type":"URL",
         |                  "hint":"This is how we'll notify you when we've processed them. It must include https and port 443",
         |                  "shortDescription" : "Callback URL",
         |                  "validation" : {
         |                    "errorMessage" : "Enter a URL in the correct format, like 'https://your.domain.name/some/path' ",
         |                    "rules" : [{
         |                      "UrlValidationRule" : {}
         |                    }]
         |                  }
         |               },
         |               {
         |                  "name":"securityToken",
         |                  "description":"What's the value of the HTTP Authorization header we should use to notify you?",
         |                  "type":"SecureToken",
         |                  "hint":"For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk",
         |                  "shortDescription" : "Authorization Token"
         |               },
         |               {
         |                  "name": "authenticatedEori",
         |                  "description": "What's your Economic Operator Registration and Identification (EORI) number?",
         |                  "type": "STRING",
         |                  "hint": "This is your EORI that will associate your application with you as a CSP",
         |                  "shortDescription" : "EORI"
         |               }
         |            ]
         |         },
         |         {
         |            "version":"2.0",
         |            "status":"BETA",
         |            "endpointsEnabled":true,
         |            "access":{
         |              "type": "PRIVATE"
         |            },
         |            "fieldDefinitions":[
         |               {
         |                  "name":"callbackUrl",
         |                  "description":"What's your callback URL for declaration submissions?",
         |                  "type":"URL",
         |                  "hint":"This is how we'll notify you when we've processed them. It must include https and port 443",
         |                  "shortDescription" : "Callback URL",
         |                  "validation" : {
         |                    "errorMessage" : "Enter a URL in the correct format, like 'https://your.domain.name/some/path' ",
         |                    "rules" : [{
         |                      "UrlValidationRule" : {}
         |                    }]
         |                  }
         |               },
         |               {
         |                  "name":"securityToken",
         |                  "description":"What's the value of the HTTP Authorization header we should use to notify you?",
         |                  "type":"SecureToken",
         |                  "hint":"For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk",
         |                  "shortDescription" : "Authorization Token"
         |               },
         |               {
         |                  "name": "authenticatedEori",
         |                  "description": "What's your Economic Operator Registration and Identification (EORI) number?",
         |                  "type": "STRING",
         |                  "hint": "This is your EORI that will associate your application with you as a CSP",
         |                  "shortDescription" : "EORI"
         |               }
         |            ]
         |         },
         |         {
         |            "version":"3.0",
         |            "status":"BETA",
         |            "endpointsEnabled":true,
         |            "access":{
         |                "type":"PRIVATE"
         |            },
         |            "fieldDefinitions":[
         |               {
         |                  "name":"callbackUrl",
         |                  "description":"What's your callback URL for declaration submissions?",
         |                  "type":"URL",
         |                  "hint":"This is how we'll notify you when we've processed them. It must include https and port 443",
         |                  "shortDescription" : "Callback URL",
         |                  "validation" : {
         |                    "errorMessage" : "Enter a URL in the correct format, like 'https://your.domain.name/some/path' ",
         |                    "rules" : [{
         |                      "UrlValidationRule" : {}
         |                    }]
         |                  }
         |               },
         |               {
         |                  "name":"securityToken",
         |                  "description":"What's the value of the HTTP Authorization header we should use to notify you?",
         |                  "type":"SecureToken",
         |                  "hint":"For example: Basic YXNkZnNhZGZzYWRmOlZLdDVOMVhk",
         |                  "shortDescription" : "Authorization Token"
         |               },
         |               {
         |                  "name": "authenticatedEori",
         |                  "description": "What's your Economic Operator Registration and Identification (EORI) number?",
         |                  "type": "STRING",
         |                  "hint": "This is your EORI that will associate your application with you as a CSP",
         |                  "shortDescription" : "EORI"
         |               }
         |            ]
         |         }
         |      ]
         |   }
         |}
      """.stripMargin)

}
