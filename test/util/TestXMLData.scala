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

import scala.xml.Elem

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

  def validBatchFileUploadXml(fileGroupSize: Int = 2, fileSequenceNo1: Int = 1, fileSequenceNo2: Int = 2): Elem =
    <FileUploadRequest
    xmlns="hmrc:batchfileupload"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <DeclarationID>declarationId</DeclarationID>
      <FileGroupSize>{fileGroupSize}</FileGroupSize>
      <Files>
        <File>
          <FileSequenceNo>{fileSequenceNo1}</FileSequenceNo>
          <DocumentType>document type {fileSequenceNo1}</DocumentType>
        </File>
        <File>
          <FileSequenceNo>{fileSequenceNo2}</FileSequenceNo>
          <DocumentType>document type {fileSequenceNo2}</DocumentType>
        </File>
      </Files>
    </FileUploadRequest>

  val InvalidFileUploadXml: Elem = <upscanInitiate xmlns="hmrc:fileupload">
    <declarationID foo="bar">dec123</declarationID>
    <documentationType>docType123</documentationType>
  </upscanInitiate>

}
