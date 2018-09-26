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

package unit.schemas

import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.services.XmlValidationService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Elem, Node, SAXException}

class StatusQueryResponseSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  protected val MockConfiguration = mock[Configuration]
  protected val MockXml = mock[Node]

  protected val propertyName: String = "xsd.locations.statusqueryresponse"

  protected val xsdLocations: Seq[String] = Seq("/api/conf/2.0/schemas/wco/status/StatusQueryResponse.xsd")

  def xmlValidationService: XmlValidationService = new XmlValidationService(MockConfiguration, schemaPropertyName = propertyName) {}

  override protected def beforeEach() {
    reset(MockConfiguration)
    when(MockConfiguration.getStringSeq(propertyName)).thenReturn(Some(xsdLocations))
    when(MockConfiguration.getInt("xml.max-errors")).thenReturn(None)
  }

  "A status query response" should {
    "be successfully validated if correct" in {
      val result = await(xmlValidationService.validate(ValidStatusQueryResponseXML))

      result should be(())
    }

    "fail validation if is incorrect" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidStatusQueryResponseXML))
      }

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'taggie'."

      Option(caught.getException) shouldBe None
    }

    "fail validation if is not filtered" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(FullStatusQueryResponseXML))
      }

      caught.getMessage shouldBe "cvc-elt.1.a: Cannot find the declaration of element 'n1:queryDeclarationInformationResponse'."

      Option(caught.getException) shouldBe None
    }
  }

  private val InvalidStatusQueryResponseXML = <taggie>content</taggie>


  private val ValidStatusQueryResponseXML: Elem =
    <stat:declarationManagementInformationResponse xmlns:stat="http://gov.uk/customs/declarations/status-request">
      <stat:declaration>
        <!--Optional:-->
        <stat:versionNumber>100</stat:versionNumber>
        <!--Optional:-->
        <stat:creationDate formatCode="string">2008-09-29T02:49:45</stat:creationDate>
        <!--Optional:-->
        <stat:goodsItemCount unitType="string" qualifier="string">1000.00</stat:goodsItemCount>
        <!--Optional:-->
        <stat:tradeMovementType type="token" responsibleAgent="token">token</stat:tradeMovementType>
        <!--Optional:-->
        <stat:type type="token" responsibleAgent="token">token</stat:type>
        <!--Optional:-->
        <stat:packageCount unitType="string" qualifier="string">1000.00</stat:packageCount>
        <!--Optional:-->
        <stat:acceptanceDate formatCode="string">2014-09-19T00:18:33</stat:acceptanceDate>
        <!--Zero or more repetitions:-->
        <stat:parties>
          <!--Optional:-->
          <stat:partyIdentification>
            <stat:number>string</stat:number>
          </stat:partyIdentification>
        </stat:parties>
      </stat:declaration>
    </stat:declarationManagementInformationResponse>


  private val FullStatusQueryResponseXML: Elem = <n1:queryDeclarationInformationResponse
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:xsd_1="http://trade.core.ecf/messages/2017/03/31/"
  xmlns:n1="http://gov.uk/customs/retrieveDeclarationInformation/v1"
  xmlns:tns="http://cmm.core.ecf/BaseTypes/cmmPartyTypes/trade/2017/02/22/"
  xmlns:tns_1="http://cmm.core.ecf/BaseTypes/cmmServiceTypes/trade/2017/02/22/"
  xmlns:n2="http://cmm.core.ecf/BaseTypes/cmmDeclarationTypes/trade/2017/02/22/"
  xmlns:tns_4="http://cmm.core.ecf/BaseTypes/cmmEnhancementTypes/trade/2017/02/22/"
  xsi:schemaLocation="http://gov.uk/customs/retrieveDeclarationInformation/v1 response_schema.xsd">

    <n1:responseCommon>
      <n1:processingDate>2016-11-30T09:31:00Z</n1:processingDate>
    </n1:responseCommon>
    <n1:responseDetail>
      <n1:declarationManagementInformationResponse>
        <tns_1:request>
          <tns_1:id>12b0a74b-13bb-42a0-b9db-8ba4ada22fd9</tns_1:id>
          <tns_1:timeStamp>2016-11-30T10:28:54.128Z</tns_1:timeStamp>
        </tns_1:request>
        <xsd_1:declaration>
          <tns_4:isCurrent>true</tns_4:isCurrent>
          <tns_4:versionNumber>1</tns_4:versionNumber>
          <tns_4:creationDate>2016-11-22T08:11:30.016Z</tns_4:creationDate>
          <tns_4:isDisplayable>true</tns_4:isDisplayable>
          <n2:totalGrossMass>90.00</n2:totalGrossMass>
          <n2:modeOfEntry>2</n2:modeOfEntry>
          <n2:goodsItemCount>1</n2:goodsItemCount>
          <n2:communicationAddress>hmrcgwid:144b80b0-b46e-4c56-be1a-83b36649ac46:a
            d3a8c50-fc1c-4b81-a56c-bb153aced791:DHL127</n2:communicationAddress>
          <n2:receiveDate>2016-11-22T15:11:30.123Z</n2:receiveDate>
          <n2:reference>523-123456A-B6780</n2:reference>
          <n2:submitterReference>SNST2487851</n2:submitterReference>
          <n2:tradeMovementType>IM</n2:tradeMovementType>
          <n2:type>D</n2:type>
          <n2:goodsCommunityStatus>1</n2:goodsCommunityStatus>
          <n2:loadingListCount>1</n2:loadingListCount>
          <n2:packageCount>3</n2:packageCount>
          <n2:acceptanceDate>2016-11-22T15:11:40.123Z</n2:acceptanceDate>
          <n2:invoiceAmount>13915.37</n2:invoiceAmount>
          <xsd_1:consignmentShipment>
            <xsd_1:customsOffices/>
            <xsd_1:invoice/>
            <xsd_1:tradeTerms/>
            <xsd_1:goodsItems>
              <n2:quotaOrderNumber/>
              <n2:methodOfPayment/>
              <n2:customsReferenceNumber/>
              <n2:valuationMethod/>
              <n2:previousProcedure/>
              <n2:requestedProcedure/>
              <n2:sequenceNumber>1</n2:sequenceNumber>
              <xsd_1:previousDocuments>
                <n2:type>MCR</n2:type>
                <n2:sequenceNumber>1</n2:sequenceNumber>
                <n2:id>2476AB127</n2:id>
              </xsd_1:previousDocuments>
              <xsd_1:ucr/>
              <xsd_1:commodity>
                <n2:sequenceNumber>1</n2:sequenceNumber>
                <n2:classifications>
                  <n2:identifier>12345561</n2:identifier>
                  <n2:sequenceNumber>1</n2:sequenceNumber>
                  <n2:type>TSP</n2:type>
                </n2:classifications>
              </xsd_1:commodity>
            </xsd_1:goodsItems>
          </xsd_1:consignmentShipment>
        </xsd_1:declaration>
      </n1:declarationManagementInformationResponse>
    </n1:responseDetail>
  </n1:queryDeclarationInformationResponse>
}
