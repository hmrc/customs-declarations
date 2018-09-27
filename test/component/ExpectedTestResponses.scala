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

package component

trait ExpectedTestResponses {

  protected val BadRequestError: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Payload is not valid according to schema</message>
      |  <errors>
      |     <error>
      |       <code>xml_validation_error</code>
      |       <message>cvc-complex-type.3.2.2: Attribute 'foo' is not allowed to appear in element 'Declaration'.</message>
      |     </error>
      |  </errors>
      |</errorResponse>
    """.stripMargin

  protected val BadRequestSchemaValidationError: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Payload is not valid according to schema</message>
      |  <errors>
      |     <error>
      |       <code>xml_validation_error</code>
      |       <message>cvc-type.3.1.1: Element 'declarationID' is a simple type, so it cannot have attributes, excepting those whose namespace name is identical to 'http://www.w3.org/2001/XMLSchema-instance' and whose [local name] is one of 'type', 'nil', 'schemaLocation' or 'noNamespaceSchemaLocation'. However, the attribute, 'foo' was found.</message>
      |     </error>
      |  </errors>
      |</errorResponse>
    """.stripMargin

  protected val BadRequestErrorIncorrectEndpoint: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |    <code>BAD_REQUEST</code> <message>Payload is not valid according to schema</message> <errors>
      |      <error>
      |        <code>xml_validation_error</code> <message>cvc-complex-type.2.4.a: Invalid content was found starting with element 'AcceptanceDateTime'. One of '{&quot;urn:wco:datamodel:WCO:DEC-DMS:2&quot;:FunctionCode}' is expected.</message>
      |      </error>
      |      <error>
      |        <code>xml_validation_error</code> <message>cvc-pattern-valid: Value '9' is not facet-valid with respect to pattern '13' for type 'FunctionCode'.</message>
      |      </error>
      |    </errors>
      |  </errorResponse>
    """.stripMargin

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
      |   <message>Declaration acceptance date is greater than 60 days old</message>
      | </errorResponse>
    """.stripMargin

  protected val BadStatusResponseErrorBadgeIdMissingOrInvalid: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      | <errorResponse>
      |   <code>BAD_REQUEST</code>
      |   <message>Badge Identifier is missing or invalid</message>
      |   </errorResponse>
      |""".stripMargin

  protected val BadRequestErrorXBadgeIdentifierMissingOrInvalid: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>X-Badge-Identifier header is missing or invalid</message>
      |</errorResponse>
    """.stripMargin

  protected val MalformedXmlBodyError: String  =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>Request body does not contain a well-formed XML document.</message>
      |</errorResponse>
    """.stripMargin

  protected val BadRequestErrorXBatchIdentifierMissingOrInvalid: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>BAD_REQUEST</code>
      |  <message>X-Badge-Identifier header is missing or invalid</message>
      |</errorResponse>
    """.stripMargin

  protected val ForbiddenError: String =
    """<errorResponse>
      |  <code>FORBIDDEN</code> <message>Not an authorized service</message>
      |</errorResponse>
    """.stripMargin

  protected val InvalidAcceptHeaderError: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>ACCEPT_HEADER_INVALID</code>
      |  <message>The accept header is missing or invalid</message>
      |</errorResponse>
    """.stripMargin

  protected val InvalidContentTypeHeaderError: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>UNSUPPORTED_MEDIA_TYPE</code>
      |  <message>The content type header is missing or invalid</message>
      |</errorResponse>
    """.stripMargin

  protected val InternalServerError: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<errorResponse>
      |  <code>INTERNAL_SERVER_ERROR</code>
      |  <message>Internal server error</message>
      |</errorResponse>
    """.stripMargin

  protected val UnauthorisedRequestError: String =
    """<errorResponse>
      |  <code>UNAUTHORIZED</code>
      |  <message>Unauthorised request</message>
      |</errorResponse>
    """.stripMargin
}
