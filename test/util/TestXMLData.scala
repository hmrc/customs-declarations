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

import org.joda.time.{DateTime, DateTimeZone}

import scala.xml.{Elem, NodeSeq}

object TestXMLData {
  val cancellationFunctionCode = 13
  val cancellationTypeCode = "INV"

  def validCancellationXML(functionCode: Int = cancellationFunctionCode, typeCode: String = cancellationTypeCode): Elem =
    <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2">
      <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      <md:WCOTypeName>DEC</md:WCOTypeName>
      <md:ResponsibleCountryCode>NL</md:ResponsibleCountryCode>
      <md:ResponsibleAgencyName>Duane</md:ResponsibleAgencyName>
      <md:AgencyAssignedCustomizationVersionCode>v2.1</md:AgencyAssignedCustomizationVersionCode>
      <Declaration>
        <FunctionCode>{functionCode}</FunctionCode>
        <FunctionalReferenceID>Danielle_20180404_1154</FunctionalReferenceID>
        <ID>18GBJFKYDPAB34VGO7</ID>
        <TypeCode>{typeCode}</TypeCode>
        <Submitter>
          <ID>NL025115165432</ID>
        </Submitter>
        <AdditionalInformation>
          <StatementDescription>This is a duplicate, please cancel</StatementDescription>
          <StatementTypeCode>CUS</StatementTypeCode>
        </AdditionalInformation>
        <Amendment>
          <ChangeReasonCode>1</ChangeReasonCode>
        </Amendment>
      </Declaration>
    </md:MetaData>

  val InvalidCancellationXML: Elem = <md:MetaData xmlns="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
    <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
    <md:WCOTypeName>DEC</md:WCOTypeName>
    <md:ResponsibleCountryCode>NL</md:ResponsibleCountryCode>
    <md:ResponsibleAgencyName>Duane</md:ResponsibleAgencyName>
    <md:AgencyAssignedCustomizationVersionCode>v2.1</md:AgencyAssignedCustomizationVersionCode>
    <Declaration foo="bar">
      <FunctionCode>13</FunctionCode>
      <FunctionalReferenceID>Danielle_20180404_1154</FunctionalReferenceID>
      <ID>18GBJFKYDPAB34VGO7</ID>
      <TypeCode>INV</TypeCode>
      <Submitter>
        <ID>NL025115165432</ID>
      </Submitter>
      <AdditionalInformation>
        <StatementDescription>This is a duplicate, please cancel</StatementDescription>
      </AdditionalInformation>
      <Amendment>
        <ChangeReasonCode>1</ChangeReasonCode>
      </Amendment>
    </Declaration>
  </md:MetaData>

  val InvalidCancellationXMLWith2Errors: Elem = <md:MetaData xmlns="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
    <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
    <md:WCOTypeName>DEC</md:WCOTypeName>
    <md:ResponsibleCountryCode>NL</md:ResponsibleCountryCode>
    <md:ResponsibleAgencyName>Duane</md:ResponsibleAgencyName>
    <md:AgencyAssignedCustomizationVersionCode>v2.1</md:AgencyAssignedCustomizationVersionCode>
    <Declaration foo="bar">
      <FunctionCode>ABC</FunctionCode>
      <FunctionalReferenceID>Danielle_20180404_1154</FunctionalReferenceID>
      <ID>18GBJFKYDPAB34VGO7</ID>
      <TypeCode>13</TypeCode>
      <Submitter>
        <ID>NL025115165432</ID>
      </Submitter>
      <AdditionalInformation>
        <StatementDescription>This is a duplicate, please cancel</StatementDescription>
      </AdditionalInformation>
      <Amendment>
        <ChangeReasonCode>1</ChangeReasonCode>
      </Amendment>
    </Declaration>
  </md:MetaData>

  val InvalidSubmissionXML: Elem =
    <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2"
                 xmlns:udt="urn:wco:datamodel:WCO:Declaration_DS:DMS:2">
      <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      <md:WCOTypeName>DEC-DMS</md:WCOTypeName>
      <md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
      <md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
      <md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>

      <Declaration foo="bar"/>
    </md:MetaData>

  val InvalidSubmissionXMLWith2Errors: Elem =
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

  val InvalidClearanceRequestXmlWith2Errors: Elem =
    <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2"
                 xmlns:udt="urn:wco:datamodel:WCO:Declaration_DS:DMS:2">
      <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      <md:WCOTypeName>DEC-DMS</md:WCOTypeName>
      <md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
      <md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
      <md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>

      <Declaration foo="bar">
        <GoodsItemQuantity>ABC</GoodsItemQuantity>
      </Declaration>
    </md:MetaData>

  val ValidClearanceXML: Elem =
    <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:udt="urn:wco:datamodel:WCO:Declaration_DS:DMS:2">
    <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
    <md:WCOTypeName>DEC-DMS</md:WCOTypeName>
    <md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
    <md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
    <md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
    <Declaration>
      <TypeCode>IMK</TypeCode>
    </Declaration>
  </md:MetaData>

  val ValidSubmissionXML: Elem =
    <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2"
                 xmlns:udt="urn:wco:datamodel:WCO:Declaration_DS:DMS:2">
      <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      <md:WCOTypeName>DEC-DMS</md:WCOTypeName>
      <md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
      <md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
      <md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
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

  def validSubmissionXML(functionCode: Int, typeCode: String): Elem =
    <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2"
                 xmlns:udt="urn:wco:datamodel:WCO:Declaration_DS:DMS:2">
      <md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      <md:WCOTypeName>DEC-DMS</md:WCOTypeName>
      <md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
      <md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
      <md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
      <Declaration>
        <AcceptanceDateTime>
          <udt:DateTimeString formatCode="304">20161207010101Z</udt:DateTimeString>
        </AcceptanceDateTime>
        <FunctionCode>{functionCode}</FunctionCode>
        <FunctionalReferenceID>DemoUK20161207_010</FunctionalReferenceID>
        <TypeCode>{typeCode}</TypeCode>
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

  val WrappedValidSubmissionXML: Elem =
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
        <v1:clientID>2bbb7b51-4632-4ebd-b989-7505e46996d1</v1:clientID>
        <v1:conversationID>c5192352-f849-4a13-9790-1e99253a2797</v1:conversationID>
        <v1:badgeIdentifier>validBadgeId</v1:badgeIdentifier>
      </v1:requestCommon>
      <v1:requestDetail>
        {ValidSubmissionXML}
      </v1:requestDetail>
    </v1:submitDeclarationRequest>

  val ValidFileUploadXml: Elem = <upscanInitiate xmlns="hmrc:fileupload">
    <declarationID>dec123</declarationID>
    <documentationType>docType123</documentationType>
  </upscanInitiate>

  val InvalidFileUploadXml: Elem = <upscanInitiate xmlns="hmrc:fileupload">
    <declarationID foo="bar">dec123</declarationID>
    <documentationType>docType123</documentationType>
  </upscanInitiate>

  val expectedDeclarationStatusPayload: Elem =
    <n1:queryDeclarationInformationRequest
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/"
    xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns_1="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/"
    xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 request_schema.xsd">
      <n1:requestCommon>
        <n1:clientID>SOME_X_CLIENT_ID</n1:clientID>
        <n1:conversationID>38400000-8cf0-11bd-b23e-10b96e4ef00d</n1:conversationID>
        <n1:correlationID>e61f8eee-812c-4b8f-b193-06aedc60dca2</n1:correlationID>
        <n1:badgeIdentifier>BADGEID123</n1:badgeIdentifier>
        <n1:dateTimeStamp>2018-09-11T10:28:54.128Z</n1:dateTimeStamp>
      </n1:requestCommon>
      <n1:requestDetail>
        <n1:declarationManagementInformationRequest>
          <tns_1:id>1b0a48a8-1259-42c9-9d6a-e797b919eb16</tns_1:id>
          <tns_1:timeStamp>2018-09-11T10:28:54.128Z</tns_1:timeStamp>
          <xsd_1:reference>theMrn</xsd_1:reference>
        </n1:declarationManagementInformationRequest>
      </n1:requestDetail>
    </n1:queryDeclarationInformationRequest>

  def invalidStatusResponse(declarationNode: NodeSeq): NodeSeq = <n1:queryDeclarationInformationResponse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/" xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns="http://cmm.core.ecf/BaseTypes/cmmPartyTypes/trade/2017/02/22/" xmlns:n2="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/" xmlns:n3="http://cmm.core.ecf/BaseTypes/cmmDeclarationTypes/trade/2017/02/22/" xmlns:tns_3="http://cmm.core.ecf/BaseTypes/cmmEnhancementTypes/trade/2017/02/22/" xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 queryDeclarationInformationResponse.xsd">
    <n1:responseCommon>
      <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
    </n1:responseCommon>
    <n1:responseDetail>
      <n1:declarationManagementInformationResponse>
        <n2:extensions>
          <tns_3:value>String</tns_3:value>
          <tns_3:type>token</tns_3:type>
        </n2:extensions>
        <n2:sequenceNumber>0</n2:sequenceNumber>
        <n2:request>
          <n2:status>token</n2:status>
          <n2:id>String</n2:id>
          <n2:sequenceNumber>0</n2:sequenceNumber>
          <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
          <n2:externalId>String</n2:externalId>
        </n2:request>
        <n2:id>String</n2:id>
        <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
        <n2:isFinal>true</n2:isFinal>
        <n2:externalId>String</n2:externalId>
          {declarationNode}
      </n1:declarationManagementInformationResponse>
    </n1:responseDetail>
  </n1:queryDeclarationInformationResponse>

  def statusResponseDeclarationXmlNodeNoDate: Elem =    <xsd_1:declaration>
    <n3:communicationAddress>hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:ad3a8c50-fc1c-4b81-a56cbb153aced791:BADGEID123</n3:communicationAddress>
  </xsd_1:declaration>

  def statusResponseDeclarationXmlNodeInvalidDate: Elem =    <xsd_1:declaration>
    <n3:communicationAddress>hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:ad3a8c50-fc1c-4b81-a56cbb153aced791:BADGEID123</n3:communicationAddress>
    <n3:receiveDate>2002-05-30T09:29:47.063Z</n3:receiveDate>
  </xsd_1:declaration>

  def statusResponseDeclarationXmlNodeCommunicationAddress: Elem =    <xsd_1:declaration>
    <n3:receiveDate>{DateTime.now(DateTimeZone.UTC).minusMonths(2).toString}</n3:receiveDate>
  </xsd_1:declaration>

  def statusResponseDeclarationXmlNodeCommunicationAddressFormatInvalid: Elem =    <xsd_1:declaration>
    <n3:communicationAddress>144b80b0-b46e-4c56-be1a-83b36649ac46:ad3a8c50-fc1c-4b81-a56cbb153aced791:BADGEID123</n3:communicationAddress>
    <n3:receiveDate>{DateTime.now(DateTimeZone.UTC).minusMonths(2).toString}</n3:receiveDate>
  </xsd_1:declaration>

  def validStatusResponse(receivedDate : String = DateTime.now().toString): NodeSeq = <n1:queryDeclarationInformationResponse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/" xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns="http://cmm.core.ecf/BaseTypes/cmmPartyTypes/trade/2017/02/22/" xmlns:n2="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/" xmlns:n3="http://cmm.core.ecf/BaseTypes/cmmDeclarationTypes/trade/2017/02/22/" xmlns:tns_3="http://cmm.core.ecf/BaseTypes/cmmEnhancementTypes/trade/2017/02/22/" xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 queryDeclarationInformationResponse.xsd">
    <n1:responseCommon>
      <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
    </n1:responseCommon>
    <n1:responseDetail>
      <n1:declarationManagementInformationResponse>
        <n2:extensions>
          <tns_3:value>String</tns_3:value>
          <tns_3:type>token</tns_3:type>
        </n2:extensions>
        <n2:sequenceNumber>0</n2:sequenceNumber>
        <n2:request>
          <n2:status>token</n2:status>
          <n2:id>String</n2:id>
          <n2:sequenceNumber>0</n2:sequenceNumber>
          <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
          <n2:externalId>String</n2:externalId>
        </n2:request>
        <n2:id>String</n2:id>
        <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
        <n2:isFinal>true</n2:isFinal>
        <n2:externalId>String</n2:externalId>
        <xsd_1:declaration>
          <tns_3:isCurrent>true</tns_3:isCurrent>
          <tns_3:versionNumber>0</tns_3:versionNumber>
          <tns_3:creationDate formatCode="string">2001-12-17T09:30:47Z</tns_3:creationDate>
          <tns_3:isDisplayable>true</tns_3:isDisplayable>
          <n3:extensions>
            <tns_3:value>String</tns_3:value>
            <tns_3:type>token</tns_3:type>
          </n3:extensions>
          <n3:totalGrossMass>0</n3:totalGrossMass>
          <n3:modeOfEntry>token</n3:modeOfEntry>
          <n3:goodsItemCount>2</n3:goodsItemCount>
          <n3:communicationAddress>hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:ad3a8c50-fc1c-4b81-a56cbb153aced791:BADGEID123</n3:communicationAddress>
          <n3:receiveDate>{receivedDate}</n3:receiveDate>
          <n3:reference>String</n3:reference>
          <n3:submitterReference>String</n3:submitterReference>
          <n3:tradeMovementType>trade movement type</n3:tradeMovementType>
          <n3:type>declaration type</n3:type>
          <n3:goodsCommunityStatus>token</n3:goodsCommunityStatus>
          <n3:loadingListCount>0</n3:loadingListCount>
          <n3:packageCount>3</n3:packageCount>
          <n3:acceptanceDate>2002-12-17T09:30:47Z</n3:acceptanceDate>
          <n3:invoiceAmount>0</n3:invoiceAmount>
          <n3:procedureCategory>token</n3:procedureCategory>
          <n3:batchId>String</n3:batchId>
          <n3:specificCircumstance>token</n3:specificCircumstance>
          <xsd_1:additionalDocuments>
            <n3:amount>0</n3:amount>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:quantity>0</n3:quantity>
            <n3:identifier>String</n3:identifier>
            <n3:type>token</n3:type>
            <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
            <n3:issuerName>String</n3:issuerName>
            <n3:exemption>token</n3:exemption>
            <n3:name>String</n3:name>
          </xsd_1:additionalDocuments>
          <xsd_1:additionalInformation>
            <n3:limitDate>2001-12-17T09:30:47Z</n3:limitDate>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:text>String</n3:text>
            <n3:type>token</n3:type>
            <n3:code>String</n3:code>
            <n3:pointer>String</n3:pointer>
          </xsd_1:additionalInformation>
          <xsd_1:amendments>
            <n3:declarationVersion>0</n3:declarationVersion>
            <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:reasonText>String</n3:reasonText>
            <n3:reason>token</n3:reason>
            <n3:pointer>String</n3:pointer>
          </xsd_1:amendments>
          <xsd_1:countryRegions>
            <n3:type>token</n3:type>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:countrySubDivisionId>token</n3:countrySubDivisionId>
            <n3:country>token</n3:country>
            <n3:countryGroup>token</n3:countryGroup>
            <n3:subRole>token</n3:subRole>
          </xsd_1:countryRegions>
          <xsd_1:currencies>
            <tns:exchangeRate>0</tns:exchangeRate>
            <tns:sequenceNumber>0</tns:sequenceNumber>
            <tns:code>token</tns:code>
          </xsd_1:currencies>
          <xsd_1:customsOffices>
            <n3:customsOfficeName>String</n3:customsOfficeName>
            <n3:type>token</n3:type>
            <xsd_1:customsOfficeID>
              <tns:type>token</tns:type>
              <tns:number>String</tns:number>
            </xsd_1:customsOfficeID>
          </xsd_1:customsOffices>
          <xsd_1:guarantees>
            <n3:partyID>String</n3:partyID>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:grn>String</n3:grn>
            <n3:otherReference>String</n3:otherReference>
            <n3:type>token</n3:type>
            <n3:dutyAmount>0</n3:dutyAmount>
            <n3:accessCode>String</n3:accessCode>
            <xsd_1:customsOfficeID>
              <tns:type>token</tns:type>
              <tns:number>String</tns:number>
            </xsd_1:customsOfficeID>
          </xsd_1:guarantees>
          <xsd_1:incidents>
            <n3:text>String</n3:text>
            <n3:sequenceNumber>0</n3:sequenceNumber>
          </xsd_1:incidents>
          <xsd_1:parties>
            <n3:status>token</n3:status>
            <n3:type>token</n3:type>
            <n3:partyName>String</n3:partyName>
            <n3:subRole>token</n3:subRole>
            <n3:authorizationType>token</n3:authorizationType>
            <xsd_1:partyIdentification>
              <tns:type>token</tns:type>
              <tns:number>1</tns:number>
            </xsd_1:partyIdentification>
            <xsd_1:contactPerson>
              <n3:name>String</n3:name>
              <xsd_1:contactMechanisms>
                <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
                <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
                <tns:identification>String</tns:identification>
                <tns:purpose>token</tns:purpose>
                <tns:type>token</tns:type>
              </xsd_1:contactMechanisms>
              <xsd_1:physicalAddress>
                <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
                <tns:type>token</tns:type>
                <tns:streetAndNumber>String</tns:streetAndNumber>
                <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
                <tns:cityName>String</tns:cityName>
                <tns:zipCode>String</tns:zipCode>
                <tns:houseNumberExtension>String</tns:houseNumberExtension>
                <tns:houseNumber>0</tns:houseNumber>
                <tns:streetName>String</tns:streetName>
                <tns:country>
                  <tns:code>token</tns:code>
                </tns:country>
              </xsd_1:physicalAddress>
            </xsd_1:contactPerson>
            <xsd_1:contactMechanisms>
              <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
              <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
              <tns:identification>String</tns:identification>
              <tns:purpose>token</tns:purpose>
              <tns:type>token</tns:type>
            </xsd_1:contactMechanisms>
            <xsd_1:physicalAddress>
              <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
              <tns:type>token</tns:type>
              <tns:streetAndNumber>String</tns:streetAndNumber>
              <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
              <tns:cityName>String</tns:cityName>
              <tns:zipCode>String</tns:zipCode>
              <tns:houseNumberExtension>String</tns:houseNumberExtension>
              <tns:houseNumber>0</tns:houseNumber>
              <tns:streetName>String</tns:streetName>
              <tns:country>
                <tns:code>token</tns:code>
              </tns:country>
            </xsd_1:physicalAddress>
          </xsd_1:parties>
          <xsd_1:signature>
            <n3:sign>UjBsR09EbGhjZ0dTQUxNQUFBUUNBRU1tQ1p0dU1GUXhEUzhi</n3:sign>
            <n3:name>String</n3:name>
            <n3:place>String</n3:place>
            <n3:timeStamp>2001-12-17T09:30:47Z</n3:timeStamp>
            <n3:authentication>String</n3:authentication>
          </xsd_1:signature>
          <xsd_1:consignmentShipment>
            <n3:extensions>
              <tns_3:value>String</tns_3:value>
              <tns_3:type>token</tns_3:type>
            </n3:extensions>
            <n3:specificCircumstance>token</n3:specificCircumstance>
            <n3:transactionNature>token</n3:transactionNature>
            <n3:goodsShippedByContainer>true</n3:goodsShippedByContainer>
            <n3:exitDate>2001-12-17T09:30:47Z</n3:exitDate>
            <n3:postalCharges>0</n3:postalCharges>
            <xsd_1:additionalInformation>
              <n3:limitDate>2001-12-17T09:30:47Z</n3:limitDate>
              <n3:sequenceNumber>0</n3:sequenceNumber>
              <n3:text>String</n3:text>
              <n3:type>token</n3:type>
              <n3:code>String</n3:code>
              <n3:pointer>String</n3:pointer>
            </xsd_1:additionalInformation>
            <xsd_1:containers>
              <n3:sequenceNumber>0</n3:sequenceNumber>
              <n3:id>String</n3:id>
              <n3:type>token</n3:type>
              <xsd_1:seals>
                <n3:id>String</n3:id>
                <n3:sequenceNumber>0</n3:sequenceNumber>
              </xsd_1:seals>
            </xsd_1:containers>
            <xsd_1:countryRegions>
              <n3:type>token</n3:type>
              <n3:sequenceNumber>0</n3:sequenceNumber>
              <n3:countrySubDivisionId>token</n3:countrySubDivisionId>
              <n3:country>token</n3:country>
              <n3:countryGroup>token</n3:countryGroup>
              <n3:subRole>token</n3:subRole>
            </xsd_1:countryRegions>
            <xsd_1:customsOffices>
              <n3:customsOfficeName>String</n3:customsOfficeName>
              <n3:type>token</n3:type>
              <xsd_1:customsOfficeID>
                <tns:type>token</tns:type>
                <tns:number>String</tns:number>
              </xsd_1:customsOfficeID>
            </xsd_1:customsOffices>
            <xsd_1:customsWareHouse>
              <n3:warehouseName>String</n3:warehouseName>
              <n3:warehouseID>String</n3:warehouseID>
              <n3:warehouseType>token</n3:warehouseType>
            </xsd_1:customsWareHouse>
            <xsd_1:freight>
              <n3:paymentMethod>token</n3:paymentMethod>
              <n3:amount>0</n3:amount>
            </xsd_1:freight>
            <xsd_1:invoice>
              <n3:date>2001-12-17T09:30:47Z</n3:date>
              <n3:id>String</n3:id>
            </xsd_1:invoice>
            <xsd_1:locations>
              <n3:locationName>String</n3:locationName>
              <n3:locationId>String</n3:locationId>
              <n3:type>token</n3:type>
              <n3:locationIdentificationType>token</n3:locationIdentificationType>
              <n3:locationType>token</n3:locationType>
              <n3:locationAdditionalId>String</n3:locationAdditionalId>
              <xsd_1:dateTimes>
                <n3:type>token</n3:type>
                <n3:dateTime>2001-12-17T09:30:47Z</n3:dateTime>
              </xsd_1:dateTimes>
              <xsd_1:physicalAddress>
                <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
                <tns:type>token</tns:type>
                <tns:streetAndNumber>String</tns:streetAndNumber>
                <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
                <tns:cityName>String</tns:cityName>
                <tns:zipCode>String</tns:zipCode>
                <tns:houseNumberExtension>String</tns:houseNumberExtension>
                <tns:houseNumber>0</tns:houseNumber>
                <tns:streetName>String</tns:streetName>
                <tns:country>
                  <tns:code>token</tns:code>
                </tns:country>
              </xsd_1:physicalAddress>
            </xsd_1:locations>
            <xsd_1:parties>
              <n3:status>token</n3:status>
              <n3:type>token</n3:type>
              <n3:partyName>String</n3:partyName>
              <n3:subRole>token</n3:subRole>
              <n3:authorizationType>token</n3:authorizationType>
              <xsd_1:partyIdentification>
                <tns:type>token</tns:type>
                <tns:number>2</tns:number>
              </xsd_1:partyIdentification>
              <xsd_1:contactPerson>
                <n3:name>String</n3:name>
                <xsd_1:contactMechanisms>
                  <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
                  <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
                  <tns:identification>String</tns:identification>
                  <tns:purpose>token</tns:purpose>
                  <tns:type>token</tns:type>
                </xsd_1:contactMechanisms>
                <xsd_1:physicalAddress>
                  <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
                  <tns:type>token</tns:type>
                  <tns:streetAndNumber>String</tns:streetAndNumber>
                  <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
                  <tns:cityName>String</tns:cityName>
                  <tns:zipCode>String</tns:zipCode>
                  <tns:houseNumberExtension>String</tns:houseNumberExtension>
                  <tns:houseNumber>0</tns:houseNumber>
                  <tns:streetName>String</tns:streetName>
                  <tns:country>
                    <tns:code>token</tns:code>
                  </tns:country>
                </xsd_1:physicalAddress>
              </xsd_1:contactPerson>
              <xsd_1:contactMechanisms>
                <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
                <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
                <tns:identification>String</tns:identification>
                <tns:purpose>token</tns:purpose>
                <tns:type>token</tns:type>
              </xsd_1:contactMechanisms>
              <xsd_1:physicalAddress>
                <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
                <tns:type>token</tns:type>
                <tns:streetAndNumber>String</tns:streetAndNumber>
                <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
                <tns:cityName>String</tns:cityName>
                <tns:zipCode>String</tns:zipCode>
                <tns:houseNumberExtension>String</tns:houseNumberExtension>
                <tns:houseNumber>0</tns:houseNumber>
                <tns:streetName>String</tns:streetName>
                <tns:country>
                  <tns:code>token</tns:code>
                </tns:country>
              </xsd_1:physicalAddress>
            </xsd_1:parties>
            <xsd_1:tradeTerms>
              <n3:description>String</n3:description>
              <n3:type>token</n3:type>
              <n3:location>token</n3:location>
              <n3:locationName>String</n3:locationName>
              <n3:situation>token</n3:situation>
            </xsd_1:tradeTerms>
            <xsd_1:transhipments>
              <n3:date>2001-12-17T09:30:47Z</n3:date>
              <xsd_1:location>
                <tns:unLocode>token</tns:unLocode>
                <tns:name>String</tns:name>
                <tns:identification>String</tns:identification>
              </xsd_1:location>
              <xsd_1:transportMeans>
                <n3:name>String</n3:name>
                <n3:mode>token</n3:mode>
                <n3:type>token</n3:type>
                <n3:role>token</n3:role>
                <n3:nationality>token</n3:nationality>
                <n3:identifier>String</n3:identifier>
                <n3:identificationType>token</n3:identificationType>
              </xsd_1:transportMeans>
              <xsd_1:transportMeans>
                <n3:name>String</n3:name>
                <n3:mode>token</n3:mode>
                <n3:type>token</n3:type>
                <n3:role>token</n3:role>
                <n3:nationality>token</n3:nationality>
                <n3:identifier>String</n3:identifier>
                <n3:identificationType>token</n3:identificationType>
              </xsd_1:transportMeans>
            </xsd_1:transhipments>
            <xsd_1:transportMeans>
              <n3:name>String</n3:name>
              <n3:mode>token</n3:mode>
              <n3:type>token</n3:type>
              <n3:role>token</n3:role>
              <n3:nationality>token</n3:nationality>
              <n3:identifier>String</n3:identifier>
              <n3:identificationType>token</n3:identificationType>
            </xsd_1:transportMeans>
            <xsd_1:ucr>
              <n3:identifier>String</n3:identifier>
              <n3:traderAssignedReference>String</n3:traderAssignedReference>
            </xsd_1:ucr>
            <xsd_1:valuationAdjustments>
              <n3:type>token</n3:type>
              <n3:sequenceNumber>0</n3:sequenceNumber>
              <n3:amount>0</n3:amount>
            </xsd_1:valuationAdjustments>
            <xsd_1:previousDocuments>
              <n3:type>token</n3:type>
              <n3:sequenceNumber>0</n3:sequenceNumber>
              <n3:lineNumber>0</n3:lineNumber>
              <n3:category>token</n3:category>
              <n3:id>String</n3:id>
            </xsd_1:previousDocuments>
            <xsd_1:goodsItems>
              <n3:extensions>
                <tns_3:value>String</tns_3:value>
                <tns_3:type>token</tns_3:type>
              </n3:extensions>
              <n3:relatedItem>0</n3:relatedItem>
              <n3:hasDV1>true</n3:hasDV1>
              <n3:quotaOrderNumber>String</n3:quotaOrderNumber>
              <n3:additionalUnits>0</n3:additionalUnits>
              <n3:isExported>true</n3:isExported>
              <n3:methodOfPayment>token</n3:methodOfPayment>
              <n3:preference>token</n3:preference>
              <n3:declaredCustomsValue>0</n3:declaredCustomsValue>
              <n3:customsReferenceNumber>String</n3:customsReferenceNumber>
              <n3:statisticalAmount>0</n3:statisticalAmount>
              <n3:adjustmentAmount>0</n3:adjustmentAmount>
              <n3:valuationMethod>token</n3:valuationMethod>
              <n3:invoiceAmount>0</n3:invoiceAmount>
              <n3:specialProcedures>token</n3:specialProcedures>
              <n3:previousProcedure>token</n3:previousProcedure>
              <n3:requestedProcedure>token</n3:requestedProcedure>
              <n3:sequenceNumber>0</n3:sequenceNumber>
              <n3:transactionNature>token</n3:transactionNature>
              <n3:valuationIndicator>token</n3:valuationIndicator>
              <n3:acceptanceDate>2001-12-17T09:30:47Z</n3:acceptanceDate>
              <xsd_1:packaging>
                <n3:netMass>0</n3:netMass>
                <n3:sequenceNumber>0</n3:sequenceNumber>
                <n3:grossMass>0</n3:grossMass>
                <n3:isShared>true</n3:isShared>
                <n3:marksNumbers>String</n3:marksNumbers>
                <n3:quantity>0</n3:quantity>
                <n3:type>token</n3:type>
                <n3:material>String</n3:material>
                <n3:volume>0</n3:volume>
                <n3:length>0</n3:length>
                <n3:width>0</n3:width>
                <n3:height>0</n3:height>
              </xsd_1:packaging>
              <xsd_1:particulars>
                <n3:text>String</n3:text>
                <n3:sequenceNumber>0</n3:sequenceNumber>
              </xsd_1:particulars>
              <xsd_1:parties>
                <n3:status>token</n3:status>
                <n3:type>token</n3:type>
                <n3:partyName>String</n3:partyName>
                <n3:subRole>token</n3:subRole>
                <n3:authorizationType>token</n3:authorizationType>
                <xsd_1:partyIdentification>
                  <tns:type>token</tns:type>
                  <tns:number>3</tns:number>
                </xsd_1:partyIdentification>
                <xsd_1:contactPerson>
                  <n3:name>String</n3:name>
                  <xsd_1:contactMechanisms>
                    <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
                    <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
                    <tns:identification>String</tns:identification>
                    <tns:purpose>token</tns:purpose>
                    <tns:type>token</tns:type>
                  </xsd_1:contactMechanisms>
                  <xsd_1:physicalAddress>
                    <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
                    <tns:type>token</tns:type>
                    <tns:streetAndNumber>String</tns:streetAndNumber>
                    <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
                    <tns:cityName>String</tns:cityName>
                    <tns:zipCode>String</tns:zipCode>
                    <tns:houseNumberExtension>String</tns:houseNumberExtension>
                    <tns:houseNumber>0</tns:houseNumber>
                    <tns:streetName>String</tns:streetName>
                    <tns:country>
                      <tns:code>token</tns:code>
                    </tns:country>
                  </xsd_1:physicalAddress>
                </xsd_1:contactPerson>
                <xsd_1:contactMechanisms>
                  <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
                  <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
                  <tns:identification>String</tns:identification>
                  <tns:purpose>token</tns:purpose>
                  <tns:type>token</tns:type>
                </xsd_1:contactMechanisms>
                <xsd_1:physicalAddress>
                  <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
                  <tns:type>token</tns:type>
                  <tns:streetAndNumber>String</tns:streetAndNumber>
                  <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
                  <tns:cityName>String</tns:cityName>
                  <tns:zipCode>String</tns:zipCode>
                  <tns:houseNumberExtension>String</tns:houseNumberExtension>
                  <tns:houseNumber>0</tns:houseNumber>
                  <tns:streetName>String</tns:streetName>
                  <tns:country>
                    <tns:code>token</tns:code>
                  </tns:country>
                </xsd_1:physicalAddress>
              </xsd_1:parties>
              <xsd_1:additionalDocuments>
                <n3:amount>0</n3:amount>
                <n3:sequenceNumber>0</n3:sequenceNumber>
                <n3:quantity>0</n3:quantity>
                <n3:identifier>String</n3:identifier>
                <n3:type>token</n3:type>
                <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
                <n3:issuerName>String</n3:issuerName>
                <n3:exemption>token</n3:exemption>
                <n3:name>String</n3:name>
              </xsd_1:additionalDocuments>
              <xsd_1:countryRegions>
                <n3:type>token</n3:type>
                <n3:sequenceNumber>0</n3:sequenceNumber>
                <n3:countrySubDivisionId>token</n3:countrySubDivisionId>
                <n3:country>token</n3:country>
                <n3:countryGroup>token</n3:countryGroup>
                <n3:subRole>token</n3:subRole>
              </xsd_1:countryRegions>
              <xsd_1:previousDocuments>
                <n3:type>token</n3:type>
                <n3:sequenceNumber>0</n3:sequenceNumber>
                <n3:lineNumber>0</n3:lineNumber>
                <n3:category>token</n3:category>
                <n3:id>String</n3:id>
              </xsd_1:previousDocuments>
              <xsd_1:valuationAdjustments>
                <n3:type>token</n3:type>
                <n3:sequenceNumber>0</n3:sequenceNumber>
                <n3:amount>0</n3:amount>
              </xsd_1:valuationAdjustments>
              <xsd_1:additionalInformation>
                <n3:limitDate>2001-12-17T09:30:47Z</n3:limitDate>
                <n3:sequenceNumber>0</n3:sequenceNumber>
                <n3:text>String</n3:text>
                <n3:type>token</n3:type>
                <n3:code>String</n3:code>
                <n3:pointer>String</n3:pointer>
              </xsd_1:additionalInformation>
              <xsd_1:declaredDutyTaxFees>
                <n3:methodOfPayment>token</n3:methodOfPayment>
                <n3:sequenceNumber>0</n3:sequenceNumber>
                <n3:reliefAmount>0</n3:reliefAmount>
                <n3:preference>token</n3:preference>
                <n3:totalAmount>0</n3:totalAmount>
                <n3:adValoremBase>0</n3:adValoremBase>
                <n3:rate>0</n3:rate>
                <n3:type>token</n3:type>
                <n3:quotaOrderNumber>String</n3:quotaOrderNumber>
                <n3:specificBase>0</n3:specificBase>
                <n3:payableAmount>0</n3:payableAmount>
              </xsd_1:declaredDutyTaxFees>
              <xsd_1:ucr>
                <n3:identifier>String</n3:identifier>
                <n3:traderAssignedReference>String</n3:traderAssignedReference>
              </xsd_1:ucr>
              <xsd_1:commodity>
                <n3:sequenceNumber>0</n3:sequenceNumber>
                <n3:description>String</n3:description>
                <n3:classifications>
                  <n3:identifier>String</n3:identifier>
                  <n3:sequenceNumber>0</n3:sequenceNumber>
                  <n3:type>token</n3:type>
                </n3:classifications>
                <n3:grossMass>0</n3:grossMass>
                <n3:netMass>0</n3:netMass>
                <n3:supplementaryUnits>0</n3:supplementaryUnits>
                <xsd_1:containers>
                  <n3:sequenceNumber>0</n3:sequenceNumber>
                  <n3:id>String</n3:id>
                  <n3:type>token</n3:type>
                  <xsd_1:seals>
                    <n3:id>String</n3:id>
                    <n3:sequenceNumber>0</n3:sequenceNumber>
                  </xsd_1:seals>
                </xsd_1:containers>
              </xsd_1:commodity>
            </xsd_1:goodsItems>
          </xsd_1:consignmentShipment>
        </xsd_1:declaration>
      </n1:declarationManagementInformationResponse>
    </n1:responseDetail>
  </n1:queryDeclarationInformationResponse>

  val generateValidStatusResponseWithMultiplePartiesOnly: NodeSeq = <n1:queryDeclarationInformationResponse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/" xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns="http://cmm.core.ecf/BaseTypes/cmmPartyTypes/trade/2017/02/22/" xmlns:n2="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/" xmlns:n3="http://cmm.core.ecf/BaseTypes/cmmDeclarationTypes/trade/2017/02/22/" xmlns:tns_3="http://cmm.core.ecf/BaseTypes/cmmEnhancementTypes/trade/2017/02/22/" xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 queryDeclarationInformationResponse.xsd">
    <n1:responseCommon>
      <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
    </n1:responseCommon>
    <n1:responseDetail>
      <n1:declarationManagementInformationResponse>
        <n2:extensions>
          <tns_3:value>String</tns_3:value>
          <tns_3:type>token</tns_3:type>
        </n2:extensions>
        <n2:sequenceNumber>0</n2:sequenceNumber>
        <n2:request>
          <n2:status>token</n2:status>
          <n2:id>String</n2:id>
          <n2:sequenceNumber>0</n2:sequenceNumber>
          <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
          <n2:externalId>String</n2:externalId>
        </n2:request>
        <n2:id>String</n2:id>
        <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
        <n2:isFinal>true</n2:isFinal>
        <n2:externalId>String</n2:externalId>
        <xsd_1:declaration>
          <tns_3:isCurrent>true</tns_3:isCurrent>
          <tns_3:isDisplayable>true</tns_3:isDisplayable>
          <n3:extensions>
            <tns_3:value>String</tns_3:value>
            <tns_3:type>token</tns_3:type>
          </n3:extensions>
          <n3:totalGrossMass>0</n3:totalGrossMass>
          <n3:modeOfEntry>token</n3:modeOfEntry>
          <n3:communicationAddress>hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:ad3a8c50-fc1c-4b81-a56cbb153aced791:BADGEID123</n3:communicationAddress>
          <n3:receiveDate>2001-12-17T09:30:47Z</n3:receiveDate>
          <n3:reference>String</n3:reference>
          <n3:submitterReference>String</n3:submitterReference>
          <n3:goodsCommunityStatus>token</n3:goodsCommunityStatus>
          <n3:loadingListCount>0</n3:loadingListCount>
          <n3:invoiceAmount>0</n3:invoiceAmount>
          <n3:procedureCategory>token</n3:procedureCategory>
          <n3:batchId>String</n3:batchId>
          <n3:specificCircumstance>token</n3:specificCircumstance>
          <xsd_1:additionalDocuments>
            <n3:amount>0</n3:amount>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:quantity>0</n3:quantity>
            <n3:identifier>String</n3:identifier>
            <n3:type>token</n3:type>
            <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
            <n3:issuerName>String</n3:issuerName>
            <n3:exemption>token</n3:exemption>
            <n3:name>String</n3:name>
          </xsd_1:additionalDocuments>
          <xsd_1:additionalInformation>
            <n3:limitDate>2001-12-17T09:30:47Z</n3:limitDate>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:text>String</n3:text>
            <n3:type>token</n3:type>
            <n3:code>String</n3:code>
            <n3:pointer>String</n3:pointer>
          </xsd_1:additionalInformation>
          <xsd_1:amendments>
            <n3:declarationVersion>0</n3:declarationVersion>
            <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:reasonText>String</n3:reasonText>
            <n3:reason>token</n3:reason>
            <n3:pointer>String</n3:pointer>
          </xsd_1:amendments>
          <xsd_1:countryRegions>
            <n3:type>token</n3:type>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:countrySubDivisionId>token</n3:countrySubDivisionId>
            <n3:country>token</n3:country>
            <n3:countryGroup>token</n3:countryGroup>
            <n3:subRole>token</n3:subRole>
          </xsd_1:countryRegions>
          <xsd_1:currencies>
            <tns:exchangeRate>0</tns:exchangeRate>
            <tns:sequenceNumber>0</tns:sequenceNumber>
            <tns:code>token</tns:code>
          </xsd_1:currencies>
          <xsd_1:customsOffices>
            <n3:customsOfficeName>String</n3:customsOfficeName>
            <n3:type>token</n3:type>
            <xsd_1:customsOfficeID>
              <tns:type>token</tns:type>
              <tns:number>String</tns:number>
            </xsd_1:customsOfficeID>
          </xsd_1:customsOffices>
          <xsd_1:guarantees>
            <n3:partyID>String</n3:partyID>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:grn>String</n3:grn>
            <n3:otherReference>String</n3:otherReference>
            <n3:type>token</n3:type>
            <n3:dutyAmount>0</n3:dutyAmount>
            <n3:accessCode>String</n3:accessCode>
            <xsd_1:customsOfficeID>
              <tns:type>token</tns:type>
              <tns:number>String</tns:number>
            </xsd_1:customsOfficeID>
          </xsd_1:guarantees>
          <xsd_1:incidents>
            <n3:text>String</n3:text>
            <n3:sequenceNumber>0</n3:sequenceNumber>
          </xsd_1:incidents>
          <xsd_1:parties>
            <n3:status>token</n3:status>
            <n3:type>token</n3:type>
            <n3:partyName>String</n3:partyName>
            <n3:subRole>token</n3:subRole>
            <n3:authorizationType>token</n3:authorizationType>
            <xsd_1:partyIdentification>
              <tns:type>token</tns:type>
              <tns:number>1</tns:number>
            </xsd_1:partyIdentification>
            <xsd_1:contactPerson>
              <n3:name>String</n3:name>
              <xsd_1:contactMechanisms>
                <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
                <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
                <tns:identification>String</tns:identification>
                <tns:purpose>token</tns:purpose>
                <tns:type>token</tns:type>
              </xsd_1:contactMechanisms>
              <xsd_1:physicalAddress>
                <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
                <tns:type>token</tns:type>
                <tns:streetAndNumber>String</tns:streetAndNumber>
                <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
                <tns:cityName>String</tns:cityName>
                <tns:zipCode>String</tns:zipCode>
                <tns:houseNumberExtension>String</tns:houseNumberExtension>
                <tns:houseNumber>0</tns:houseNumber>
                <tns:streetName>String</tns:streetName>
                <tns:country>
                  <tns:code>token</tns:code>
                </tns:country>
              </xsd_1:physicalAddress>
            </xsd_1:contactPerson>
            <xsd_1:contactMechanisms>
              <tns_3:invalidDate>2001-12-17T09:30:47Z</tns_3:invalidDate>
              <tns_3:effectiveDate>2001-12-17T09:30:47Z</tns_3:effectiveDate>
              <tns:identification>String</tns:identification>
              <tns:purpose>token</tns:purpose>
              <tns:type>token</tns:type>
            </xsd_1:contactMechanisms>
            <xsd_1:physicalAddress>
              <tns:countrySubDivisionId>String</tns:countrySubDivisionId>
              <tns:type>token</tns:type>
              <tns:streetAndNumber>String</tns:streetAndNumber>
              <tns:countrySubDivisionName>String</tns:countrySubDivisionName>
              <tns:cityName>String</tns:cityName>
              <tns:zipCode>String</tns:zipCode>
              <tns:houseNumberExtension>String</tns:houseNumberExtension>
              <tns:houseNumber>0</tns:houseNumber>
              <tns:streetName>String</tns:streetName>
              <tns:country>
                <tns:code>token</tns:code>
              </tns:country>
            </xsd_1:physicalAddress>
          </xsd_1:parties>
          <xsd_1:parties>
            <xsd_1:partyIdentification>
              <tns:number>2</tns:number>
            </xsd_1:partyIdentification>
          </xsd_1:parties>
          <xsd_1:parties></xsd_1:parties>
          <xsd_1:signature>
            <n3:sign>UjBsR09EbGhjZ0dTQUxNQUFBUUNBRU1tQ1p0dU1GUXhEUzhi</n3:sign>
            <n3:name>String</n3:name>
            <n3:place>String</n3:place>
            <n3:timeStamp>2001-12-17T09:30:47Z</n3:timeStamp>
            <n3:authentication>String</n3:authentication>
          </xsd_1:signature>
        </xsd_1:declaration>
      </n1:declarationManagementInformationResponse>
    </n1:responseDetail>
  </n1:queryDeclarationInformationResponse>

  val generateValidStatusResponseNoStatusValues: NodeSeq = <n1:queryDeclarationInformationResponse xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/" xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1" xmlns:tns="http://cmm.core.ecf/BaseTypes/cmmPartyTypes/trade/2017/02/22/" xmlns:n2="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/" xmlns:n3="http://cmm.core.ecf/BaseTypes/cmmDeclarationTypes/trade/2017/02/22/" xmlns:tns_3="http://cmm.core.ecf/BaseTypes/cmmEnhancementTypes/trade/2017/02/22/" xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 queryDeclarationInformationResponse.xsd">
    <n1:responseCommon>
      <n1:processingDate>2001-12-17T09:30:47Z</n1:processingDate>
    </n1:responseCommon>
    <n1:responseDetail>
      <n1:declarationManagementInformationResponse>
        <n2:extensions>
          <tns_3:value>String</tns_3:value>
          <tns_3:type>token</tns_3:type>
        </n2:extensions>
        <n2:sequenceNumber>0</n2:sequenceNumber>
        <n2:request>
          <n2:status>token</n2:status>
          <n2:id>String</n2:id>
          <n2:sequenceNumber>0</n2:sequenceNumber>
          <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
          <n2:externalId>String</n2:externalId>
        </n2:request>
        <n2:id>String</n2:id>
        <n2:timeStamp>2001-12-17T09:30:47Z</n2:timeStamp>
        <n2:isFinal>true</n2:isFinal>
        <n2:externalId>String</n2:externalId>
        <xsd_1:declaration>
          <tns_3:isCurrent>true</tns_3:isCurrent>
          <tns_3:isDisplayable>true</tns_3:isDisplayable>
          <n3:extensions>
            <tns_3:value>String</tns_3:value>
            <tns_3:type>token</tns_3:type>
          </n3:extensions>
          <n3:totalGrossMass>0</n3:totalGrossMass>
          <n3:modeOfEntry>token</n3:modeOfEntry>
          <n3:communicationAddress>hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:ad3a8c50-fc1c-4b81-a56cbb153aced791:BADGEID123</n3:communicationAddress>
          <n3:receiveDate>2001-12-17T09:30:47Z</n3:receiveDate>
          <n3:reference>String</n3:reference>
          <n3:submitterReference>String</n3:submitterReference>
          <n3:goodsCommunityStatus>token</n3:goodsCommunityStatus>
          <n3:loadingListCount>0</n3:loadingListCount>
          <n3:invoiceAmount>0</n3:invoiceAmount>
          <n3:procedureCategory>token</n3:procedureCategory>
          <n3:batchId>String</n3:batchId>
          <n3:specificCircumstance>token</n3:specificCircumstance>
          <xsd_1:additionalDocuments>
            <n3:amount>0</n3:amount>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:quantity>0</n3:quantity>
            <n3:identifier>String</n3:identifier>
            <n3:type>token</n3:type>
            <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
            <n3:issuerName>String</n3:issuerName>
            <n3:exemption>token</n3:exemption>
            <n3:name>String</n3:name>
          </xsd_1:additionalDocuments>
          <xsd_1:additionalInformation>
            <n3:limitDate>2001-12-17T09:30:47Z</n3:limitDate>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:text>String</n3:text>
            <n3:type>token</n3:type>
            <n3:code>String</n3:code>
            <n3:pointer>String</n3:pointer>
          </xsd_1:additionalInformation>
          <xsd_1:amendments>
            <n3:declarationVersion>0</n3:declarationVersion>
            <n3:effectiveDate>2001-12-17T09:30:47Z</n3:effectiveDate>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:reasonText>String</n3:reasonText>
            <n3:reason>token</n3:reason>
            <n3:pointer>String</n3:pointer>
          </xsd_1:amendments>
          <xsd_1:countryRegions>
            <n3:type>token</n3:type>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:countrySubDivisionId>token</n3:countrySubDivisionId>
            <n3:country>token</n3:country>
            <n3:countryGroup>token</n3:countryGroup>
            <n3:subRole>token</n3:subRole>
          </xsd_1:countryRegions>
          <xsd_1:currencies>
            <tns:exchangeRate>0</tns:exchangeRate>
            <tns:sequenceNumber>0</tns:sequenceNumber>
            <tns:code>token</tns:code>
          </xsd_1:currencies>
          <xsd_1:customsOffices>
            <n3:customsOfficeName>String</n3:customsOfficeName>
            <n3:type>token</n3:type>
            <xsd_1:customsOfficeID>
              <tns:type>token</tns:type>
              <tns:number>String</tns:number>
            </xsd_1:customsOfficeID>
          </xsd_1:customsOffices>
          <xsd_1:guarantees>
            <n3:partyID>String</n3:partyID>
            <n3:sequenceNumber>0</n3:sequenceNumber>
            <n3:grn>String</n3:grn>
            <n3:otherReference>String</n3:otherReference>
            <n3:type>token</n3:type>
            <n3:dutyAmount>0</n3:dutyAmount>
            <n3:accessCode>String</n3:accessCode>
            <xsd_1:customsOfficeID>
              <tns:type>token</tns:type>
              <tns:number>String</tns:number>
            </xsd_1:customsOfficeID>
          </xsd_1:guarantees>
          <xsd_1:incidents>
            <n3:text>String</n3:text>
            <n3:sequenceNumber>0</n3:sequenceNumber>
          </xsd_1:incidents>
          <xsd_1:signature>
            <n3:sign>UjBsR09EbGhjZ0dTQUxNQUFBUUNBRU1tQ1p0dU1GUXhEUzhi</n3:sign>
            <n3:name>String</n3:name>
            <n3:place>String</n3:place>
            <n3:timeStamp>2001-12-17T09:30:47Z</n3:timeStamp>
            <n3:authentication>String</n3:authentication>
          </xsd_1:signature>
        </xsd_1:declaration>
      </n1:declarationManagementInformationResponse>
    </n1:responseDetail>
  </n1:queryDeclarationInformationResponse>

}
