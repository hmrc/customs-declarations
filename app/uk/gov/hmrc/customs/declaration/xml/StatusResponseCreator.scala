package uk.gov.hmrc.customs.declaration.xml

import javax.inject.Singleton

import uk.gov.hmrc.customs.declaration.model.StatusResponse

import scala.xml.NodeSeq

@Singleton
class StatusResponseCreator {

  def create(statusResponse: StatusResponse): NodeSeq = {

    //TODO substitute correct xml
    <v1:submitDeclarationRequest
    xmlns:v1="http://uk/gov/hmrc/mdg/declarationmanagement/submitdeclaration/request/schema/v1"
    xmlns:n1="urn:wco:datamodel:WCO:DEC-DMS:2"
    xmlns:p1="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <v1:requestCommon>
        <v1:regime>CDS</v1:regime>
        <v1:receiptDate>{ statusResponse.creationDate}</v1:receiptDate>
      </v1:requestCommon>
    </v1:submitDeclarationRequest>
  }
}
