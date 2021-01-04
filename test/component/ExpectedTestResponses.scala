/*
 * Copyright 2021 HM Revenue & Customs
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

package component

trait ExpectedTestResponses {

  protected val BadRequestErrorWith2Errors: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Payload is not valid according to schema</message>
      |  <errors>
      |     <error>
      |       <code>xml_validation_error</code>
      |       <message>cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'.</message>
      |     </error>
      |     <error>
      |       <code>xml_validation_error</code>
      |       <message>cvc-pattern-valid: Value 'ABC' is not facet-valid with respect to pattern '13' for type 'FunctionCode'.</message>
      |     </error>
      |  </errors>
      |</errorResponse>
    """.stripMargin

  protected val BadStatusResponseErrorInvalidDate: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      | <errorResponse>
      |   <code>BAD_REQUEST</code>
      |   <message>Declaration acceptance date is greater than 90 days old</message>
      | </errorResponse>
    """.stripMargin

  protected val BadStatusResponseErrorBadgeIdMissingOrInvalid: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      | <errorResponse>
      |   <code>BAD_REQUEST</code>
      |   <message>Badge Identifier is missing or invalid</message>
      |   </errorResponse>
      |""".stripMargin

  protected val MalformedXmlBodyError: String  =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Request body does not contain a well-formed XML document.</message>
      |</errorResponse>
    """.stripMargin

  protected val UnauthorisedRequestError: String =
    """<errorResponse>
      |  <code>UNAUTHORIZED</code>
      |  <message>Unauthorised request</message>
      |</errorResponse>
    """.stripMargin
    
  protected val ServiceUnavailableError: String =
    """<?xml version='1.0' encoding='UTF-8'?>
      |<errorResponse>
      |      <code>SERVER_ERROR</code>
      |      <message>Service Unavailable</message>
      |</errorResponse>
    """.stripMargin
}
