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

package util

import java.util.UUID

import com.google.inject.AbstractModule
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceableModule
import play.api.mvc.{AnyContentAsText, AnyContentAsXml}
import play.api.test.FakeRequest
import play.api.test.Helpers.POST
import uk.gov.hmrc.customs.declaration.model.{ConversationId, Eori, Ids}
import uk.gov.hmrc.customs.declaration.services.UuidService
import util.RequestHeaders._

import scala.xml.Elem

// TODO: re-organise TestsData, with only common stuff in TestData
object TestData {
  val conversationIdValue = "38400000-8cf0-11bd-b23e-10b96e4ef00d"
  val conversationIdUuid: UUID = UUID.fromString(conversationIdValue)
  val conversationId: ConversationId = ConversationId(conversationIdValue)
  val ids = Ids(conversationId, Some(ApiSubscriptionFieldsTestData.fieldsId))

  val cspBearerToken = "CSP-Bearer-Token"
  val nonCspBearerToken = "Software-House-Bearer-Token"

  val declarantEoriValue = "ZZ123456789000"
  val declarantEori = Eori(declarantEoriValue)

  type EmulatedServiceFailure = UnsupportedOperationException
  val emulatedServiceFailure = new EmulatedServiceFailure("Emulated service failure.")

  val mockUuidService: UuidService = MockitoSugar.mock[UuidService]

  val xsdLocations = List(
    "/api/conf/2.0/schemas/wco/declaration/DocumentMetaData_2_DMS.xsd",
    "/api/conf/2.0/schemas/wco/declaration/WCO_DEC_2_DMS.xsd")

  object TestModule extends AbstractModule {
    def configure(): Unit = {
      bind(classOf[UuidService]) toInstance mockUuidService
    }

    def asGuiceableModule: GuiceableModule = GuiceableModule.guiceable(this)
  }

  val InvalidXML: Elem =
    <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2"
                 xmlns:udt="urn:wco:datamodel:WCO:Declaration_DS:DMS:2">
      <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      <md:WCOTypeName>DEC-DMS</md:WCOTypeName>
      <md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
      <md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
      <md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>

      <Declaration foo="bar"/>
    </md:MetaData>

  val InvalidXMLWith3Errors: Elem =
    <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2"
                 xmlns:udt="urn:wco:datamodel:WCO:Declaration_DS:DMS:2">
      <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      <md:WCOTypeName>DEC-DMS</md:WCOTypeName>
      <md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
      <md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
      <md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>

      <Declaration foo="bar">
        <TotalPackageQuantity>ABC</TotalPackageQuantity>
      </Declaration>
    </md:MetaData>

  val ValidXML: Elem =
      <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2"
                   xmlns:udt="urn:wco:datamodel:WCO:Declaration_DS:DMS:2">
        <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
        <md:WCOTypeName>DEC-DMS</md:WCOTypeName>
        <md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
        <md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
        <md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
        <!--
    Import Declaration including:
    - DV1 elements
    - Quota / Preference (Add.Info with type "TRR", DutyTaxFee)
    - VAT transfer
    - Additional costs (DutyTaxFee)
    - Direct representation
    - Arrival transport means
    - Payer / Surety
    - UCR
    - Warehouse reference
    - CDIU document with quantity/amount
    - Special mention (Add.Info with type "CUS")
    - National classification
    - Relief amount (DutyTaxFee)
    - Method of payment
    - Supplementary units
    - Additional calculation units
    - Previous document
    -->
        <Declaration>
          <AcceptanceDateTime>
            <udt:DateTimeString formatCode="304">20161207010101Z</udt:DateTimeString>
          </AcceptanceDateTime>
          <FunctionCode>9</FunctionCode>
          <FunctionalReferenceID>DemoUK20161207_010</FunctionalReferenceID>
          <TypeCode>IMZ</TypeCode>
          <DeclarationOfficeID>0051</DeclarationOfficeID>
          <TotalPackageQuantity>1</TotalPackageQuantity>
          <Agent>
            <ID>ZZ123456789001</ID>
            <FunctionCode>2</FunctionCode>
          </Agent>
          <CurrencyExchange> <!-- 1094 new section-->
            <RateNumeric>1.234</RateNumeric>
          </CurrencyExchange>
          <Declarant>
            <ID>ZZ123456789000</ID>
          </Declarant>
          <GoodsShipment>
            <ExitDateTime>
              <udt:DateTimeString formatCode="304">20161207010101Z</udt:DateTimeString>
            </ExitDateTime> <!-- 1094 -->
            <TransactionNatureCode>1</TransactionNatureCode>
            <Buyer>
              <Name>Buyer name Part1Buyername Part2</Name>
              <Address>
                <CityName>Buyer City name</CityName>
                <CountryCode>NL</CountryCode>
                <Line>Buyerstreet Part1BuyerStreet Part2 7C</Line>
                <PostcodeID>8603 AV</PostcodeID>
              </Address>
            </Buyer>
            <Consignee>
              <ID>ZZ123456789002</ID>
            </Consignee>
            <Consignment>
              <ArrivalTransportMeans>
                <Name>Titanic II</Name>
                <TypeCode>1</TypeCode>
              </ArrivalTransportMeans>
              <GoodsLocation>
                <Name>3016 DR, Loods 5</Name>
              </GoodsLocation>
              <LoadingLocation>  <!-- 1094 -->
                <Name>Neverland</Name>
                <ID>1234</ID>
              </LoadingLocation>
              <TransportEquipment>
                <SequenceNumeric>1</SequenceNumeric>
                <ID>CONTAINERNUMBER17</ID>
              </TransportEquipment>
              <TransportEquipment>
                <SequenceNumeric>2</SequenceNumeric>
                <ID>CONTAINERNUMBER22</ID>
              </TransportEquipment>
            </Consignment>
            <DomesticDutyTaxParty>
              <ID>ZZ123456789003</ID>
            </DomesticDutyTaxParty>
            <ExportCountry>
              <ID>CA</ID>
            </ExportCountry>
            <GovernmentAgencyGoodsItem>
              <SequenceNumeric>1</SequenceNumeric>
              <StatisticalValueAmount>1234567</StatisticalValueAmount>
              <AdditionalDocument>
                <CategoryCode>I</CategoryCode>
                <EffectiveDateTime>
                  <udt:DateTimeString formatCode="304">20130812091112Z</udt:DateTimeString>
                </EffectiveDateTime> <!-- 1094 -->
                <ID>I003INVOERVERGEU</ID> <!-- CDD-1094 an..70 -->
                <Name>NAME_HERE</Name> <!-- CDD-1094 ADDED -->
                <TypeCode>003</TypeCode>
                <LPCOExemptionCode>123</LPCOExemptionCode> <!-- 1094 -->
              </AdditionalDocument>
              <AdditionalDocument>
                <CategoryCode>N</CategoryCode>
                <ID>N861UnivCertOrigin</ID>
                <TypeCode>861</TypeCode>
              </AdditionalDocument>
              <AdditionalInformation>
                <StatementCode>1</StatementCode><!-- not affiliated -->
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>3</StatementCode><!-- no price influence -->
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>5</StatementCode><!-- no approximate value -->
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>8</StatementCode><!-- special restrictions -->
                <StatementDescription>Special Restrictions</StatementDescription>
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>9</StatementCode><!-- no price conditions -->
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>11</StatementCode><!-- no royalties or license fees -->
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>13</StatementCode><!-- no other revenue -->
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>16</StatementCode><!-- customs decisions -->
                <StatementDescription>11NL12345678901234</StatementDescription>
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>17</StatementCode><!-- contract information -->
                <StatementDescription>Contract 12123, 24-11-2011</StatementDescription>
                <StatementTypeCode>ABC</StatementTypeCode>
              </AdditionalInformation>
              <AdditionalInformation>
                <StatementCode>90010</StatementCode>
                <StatementDescription>VERVOERDER: InterTrans</StatementDescription>
                <StatementTypeCode>CUS</StatementTypeCode>
              </AdditionalInformation>
              <Commodity>
                <Description>Inertial navigation systems</Description>
                <Classification>
                  <ID>901420209000000000</ID>
                  <IdentificationTypeCode>SRZ</IdentificationTypeCode>
                </Classification>
                <Classification>
                  <ID>9002</ID>
                  <IdentificationTypeCode>GN</IdentificationTypeCode><!--Not representative for this commodity-->
                </Classification>
                <DutyTaxFee>
                  <AdValoremTaxBaseAmount currencyID="EUR">900</AdValoremTaxBaseAmount>
                  <DutyRegimeCode>
                    100<!--Specified a Tariff Quota preference (not representative)--></DutyRegimeCode>
                  <TypeCode>B00</TypeCode>
                  <Payment>
                    <MethodCode>M</MethodCode>
                  </Payment>
                </DutyTaxFee>
              </Commodity>
              <GovernmentProcedure>
                <CurrentCode>40</CurrentCode>
                <PreviousCode>91</PreviousCode>
              </GovernmentProcedure>
              <GovernmentProcedure>
                <CurrentCode>C30</CurrentCode>
              </GovernmentProcedure>
              <Origin>
                <CountryCode>US</CountryCode>
              </Origin>
              <Packaging>
                <SequenceNumeric>1</SequenceNumeric>
                <MarksNumbersID>SHIPPING MARKS PART1 SHIPPING</MarksNumbersID>
                <QuantityQuantity>9</QuantityQuantity>
                <TypeCode>CT</TypeCode>
              </Packaging>
              <PreviousDocument>
                <ID>X355ID13</ID> <!-- 1094 -->
                <LineNumeric>1</LineNumeric>
              </PreviousDocument>
              <ValuationAdjustment>
                <AdditionCode>155</AdditionCode>
              </ValuationAdjustment>
            </GovernmentAgencyGoodsItem>
            <Invoice>
              <ID>INVOICENUMBER</ID>
              <IssueDateTime>
                <udt:DateTimeString formatCode="304">20130812091112Z</udt:DateTimeString>
              </IssueDateTime>    <!-- 1094 -->
            </Invoice>
            <Payer>
              <ID>ZZ123456789003</ID>
            </Payer>
            <Seller>
              <Name>Seller name Part1Sellername Part2</Name>
              <Address>
                <CityName>Seller City name</CityName>
                <CountryCode>MX</CountryCode>
                <Line>Sellerstreet Part1SellerStreet Part2 7C</Line>
                <PostcodeID>8603 AV</PostcodeID>
              </Address>
            </Seller>
            <Surety>
              <ID>ZZ123456789003</ID>
            </Surety>
            <TradeTerms>
              <ConditionCode>CIP</ConditionCode>
              <CountryRelationshipCode>158</CountryRelationshipCode>
              <LocationName>Rotterdam</LocationName>
            </TradeTerms>
            <UCR>
              <ID>UN1234567893123456789</ID>
              <TraderAssignedReferenceID>P198R65Q29</TraderAssignedReferenceID>
            </UCR>
            <Warehouse>
              <ID>A123456ZZ</ID>
            </Warehouse>
          </GoodsShipment>
        </Declaration>
      </md:MetaData>

  val WrappedValidXML: Elem =
    <v1:submitDeclarationRequest
    xsi:schemaLocation="http://uk/gov/hmrc/mdg/declarationmanagement/submitdeclaration/request/schema/v1 file:///C:/mdg-dev/mdg-workspace/submitDeclaration/request/SubmitDecalrationRequest-schema.xsd"
    xmlns:v1="http://uk/gov/hmrc/mdg/declarationmanagement/submitdeclaration/request/schema/v1"
    xmlns:n1="urn:wco:datamodel:WCO:DEC-DMS:2"
    xmlns:p1="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <v1:requestCommon>
        <!--type: regimeType-->
        <v1:regime>CDS</v1:regime>
        <v1:receiptDate></v1:receiptDate>
        <v1:requestParameters>
          <v1:paramName>COMMUNICATIONADDRESS</v1:paramName>
          <v1:paramValue></v1:paramValue>
        </v1:requestParameters>
        <v1:messageType>xml</v1:messageType>
      </v1:requestCommon>
      <v1:requestDetail>
        { ValidXML }
      </v1:requestDetail>
    </v1:submitDeclarationRequest>

  lazy val ValidRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ACCEPT_HMRC_XML_V2_HEADER,
                 CONTENT_TYPE_HEADER,
                 API_SUBSCRIPTION_FIELDS_ID_HEADER)
    .withXmlBody(ValidXML)

  lazy val ValidRequestWithV1AcceptHeader: FakeRequest[AnyContentAsXml] = ValidRequest
    .copyFakeRequest(headers = ValidRequest.headers.remove(ACCEPT).add(ACCEPT_HMRC_XML_V1_HEADER))

  lazy val ValidRequestWithXClientIdHeader: FakeRequest[AnyContentAsXml] = ValidRequest
    .copyFakeRequest(headers =
      ValidRequest.headers.remove(API_SUBSCRIPTION_FIELDS_ID_NAME).add(X_CLIENT_ID_HEADER))

  lazy val InvalidRequest: FakeRequest[AnyContentAsXml] = ValidRequest.withXmlBody(InvalidXML)

  lazy val InvalidRequestWith3Errors: FakeRequest[AnyContentAsXml] = InvalidRequest.withXmlBody(InvalidXMLWith3Errors)

  lazy val MalformedXmlRequest: FakeRequest[AnyContentAsText] = InvalidRequest.withTextBody("<xml><non_well_formed></xml>")

  lazy val NoAcceptHeaderRequest: FakeRequest[AnyContentAsXml] = InvalidRequest
    .copyFakeRequest(headers = InvalidRequest.headers.remove(ACCEPT))

  lazy val InvalidAcceptHeaderRequest: FakeRequest[AnyContentAsXml] = InvalidRequest
    .withHeaders(RequestHeaders.ACCEPT_HEADER_INVALID)

  lazy val InvalidContentTypeHeaderRequest: FakeRequest[AnyContentAsXml] = InvalidRequest
    .withHeaders(ACCEPT_HMRC_XML_V2_HEADER, RequestHeaders.CONTENT_TYPE_HEADER_INVALID)

  lazy val NoClientIdIdHeaderRequest: FakeRequest[AnyContentAsXml] = ValidRequest
      .copyFakeRequest(headers = InvalidRequest.headers.remove(API_SUBSCRIPTION_FIELDS_ID_NAME))

  implicit class FakeRequestOps[R](val fakeRequest: FakeRequest[R]) extends AnyVal {
    def fromCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $cspBearerToken")
    def fromNonCsp: FakeRequest[R] = fakeRequest.withHeaders(AUTHORIZATION -> s"Bearer $nonCspBearerToken")

    def postTo(endpoint: String): FakeRequest[R] = fakeRequest.copyFakeRequest(method = POST, uri = endpoint)
  }

}

object RequestHeaders {

  val X_CONVERSATION_ID_NAME = "X-Conversation-ID"
  val X_CONVERSATION_ID_HEADER: (String, String) = X_CONVERSATION_ID_NAME -> TestData.conversationId.value

  val API_SUBSCRIPTION_FIELDS_ID_NAME = "api-subscription-fields-id"
  val API_SUBSCRIPTION_FIELDS_ID_HEADER: (String, String) = API_SUBSCRIPTION_FIELDS_ID_NAME -> ApiSubscriptionFieldsTestData.fieldsIdString

  val X_CLIENT_ID_ID_NAME = "X-Client-ID"
  val X_CLIENT_ID_HEADER: (String, String) = X_CLIENT_ID_ID_NAME -> ApiSubscriptionFieldsTestData.xClientId

  val CONTENT_TYPE_HEADER: (String, String) = CONTENT_TYPE -> MimeTypes.XML
  val CONTENT_TYPE_CHARSET_VALUE = MimeTypes.XML + "; charset=UTF-8"
  val CONTENT_TYPE_HEADER_CHARSET: (String, String) = CONTENT_TYPE -> CONTENT_TYPE_CHARSET_VALUE

  val CONTENT_TYPE_HEADER_INVALID: (String, String) = CONTENT_TYPE -> "somethinginvalid"

  val ACCEPT_HMRC_XML_V1_VALUE = "application/vnd.hmrc.1.0+xml"
  val ACCEPT_HMRC_XML_V2_VALUE = "application/vnd.hmrc.2.0+xml"
  val ACCEPT_HMRC_XML_V1_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V1_VALUE
  val ACCEPT_HMRC_XML_V2_HEADER: (String, String) = ACCEPT -> ACCEPT_HMRC_XML_V2_VALUE

  val ACCEPT_HEADER_INVALID: (String, String) = ACCEPT -> "invalid"

  val AUTH_HEADER_VALUE: String = "AUTH_HEADER_VALUE"
  val AUTH_HEADER: (String, String) = HeaderNames.AUTHORIZATION -> AUTH_HEADER_VALUE

  val ValidHeaders = Map(
    CONTENT_TYPE_HEADER,
    ACCEPT_HMRC_XML_V2_HEADER,
    API_SUBSCRIPTION_FIELDS_ID_HEADER)
}
