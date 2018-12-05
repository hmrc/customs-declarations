# Customs Declarations Curl Commmands
---
### Endpoints Summary

| Path                                                                                                                            |  Method  | Description                                |
|---------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------|
| [`/`](#user-content-post-customs-declaration)                                                                                   |   `POST` |    Allows submission of a Customs Declaration |
| [`/arrival-notification`](#user-content-post-arrival-notification)                                                              |   `POST` |    Allows submission of a Customs Arrival Notification Declaration | 
| [`/cancellation-requests`](#user-content-post-cancellation-requests)                                                            |   `POST` |    Allows submission of a cancellation request |
| [`/clearance`](#user-content-post-clearance)                                                                                    |   `POST` |    Allows submission of a Customs Clearance Declaration |
| [`/amend`](#user-content-post-amend)                                                                                            |   `POST` |    Allows submission of a Customs Amend Declaration. |
| [`/status-request/mrn/{valid mrn}`](#user-content-get-status-request)                                                           |   `GET`  |    Allows requesting the status of a Declaration |
| [`/file-upload`](#user-content-post-file-upload)                                                                                |   `POST` |    Allows requests for Amazon endpoints for uploading supporting files for a declaration |

--- 
 
### POST Customs Declaration 
#### `POST /`
Submits a new customs declaration
 
 
##### curl command
```
curl -v -X POST "http://localhost:9820/" \
  -H 'Accept: application/vnd.hmrc.2.0+xml' \
  -H 'Authorization: Bearer {ADD VALID TOKEN}' \
  -H 'Content-Type: application/xml' \
  -H 'X-Badge-Identifier: {Badge Id}' \
  -H 'X-Client-ID: {Valid Client Id}' \
  -H 'cache-control: no-cache' \
 -d '<?xml version="1.0" encoding="UTF-8"?>
     <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2">
     		<md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
     		<md:WCOTypeName>DEC-DMS</md:WCOTypeName>
     		<md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
     		<md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
     		<md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
     <Declaration>
     </Declaration>
     </md:MetaData>'
```
 
---

### POST Arrival Notification 
#### `POST /arrival-notification`
Submits a customs arrival notification declaration
 
 
##### curl command
```
curl -v -X POST "http://localhost:9820/arrival-notification" \
  -H 'Accept: application/vnd.hmrc.2.0+xml' \
  -H 'Authorization: Bearer {ADD VALID TOKEN}' \
  -H 'Content-Type: application/xml' \
  -H 'X-Badge-Identifier: {Badge Id}' \
  -H 'X-Client-ID: {Valid Client Id}' \
  -H 'cache-control: no-cache' \
 -d '<?xml version="1.0" encoding="UTF-8"?>
     <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2">
     		<md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
     		<md:WCOTypeName>DEC-DMS</md:WCOTypeName>
     		<md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
     		<md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
     		<md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
     <Declaration>
     </Declaration>
     </md:MetaData>'
```
 
---
### POST Cancellation Requests 
#### `POST /cancellation-requests`
Submits a cancellation request
 
 
##### curl command
```
curl -v -X POST "http://localhost:9820/cancellation-requests" \
  -H 'Accept: application/vnd.hmrc.2.0+xml' \
  -H 'Authorization: Bearer {ADD VALID TOKEN}' \
  -H 'Content-Type: application/xml' \
  -H 'X-Badge-Identifier: {Badge Id}' \
  -H 'X-Client-ID: {Valid Client Id}' \
  -H 'cache-control: no-cache' \
 -d '<?xml version="1.0" encoding="UTF-8"?>
     <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2">
     		<md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
     		<md:WCOTypeName>DEC-DMS</md:WCOTypeName>
     		<md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
     		<md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
     		<md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
    <Declaration>
          <FunctionCode>13</FunctionCode>
            <FunctionalReferenceID>Danielle_20180404_1154</FunctionalReferenceID>
            <ID>18GBJFKYDPAB34VGO7</ID>
            <TypeCode>INV</TypeCode>
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
     </md:MetaData>'
```
 ---
 ### POST Clearance 
 #### `POST /clearance`
 Submits a clearance request
  
  
 ##### curl command
 ```
 curl -v -X POST "http://localhost:9820/clearance" \
   -H 'Accept: application/vnd.hmrc.2.0+xml' \
   -H 'Authorization: Bearer {ADD VALID TOKEN}' \
   -H 'Content-Type: application/xml' \
   -H 'X-Badge-Identifier: {Badge Id}' \
   -H 'X-Client-ID: {Valid Client Id}' \
   -H 'cache-control: no-cache' \
  -d '<?xml version="1.0" encoding="UTF-8"?>
      <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2">
      		<md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      		<md:WCOTypeName>DEC-DMS</md:WCOTypeName>
      		<md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
      		<md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
      		<md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
          <Declaration>
         </Declaration>
      </md:MetaData>'
 ```
---

 ### POST Amend 
 #### `POST /amend`
 Submits a declaration amend request
  
  
 ##### curl command
 ```
 curl -v -X POST "http://localhost:9820/amend" \
   -H 'Accept: application/vnd.hmrc.2.0+xml' \
   -H 'Authorization: Bearer {ADD VALID TOKEN}' \
   -H 'Content-Type: application/xml' \
   -H 'X-Badge-Identifier: {Badge Id}' \
   -H 'X-Client-ID: {Valid Client Id}' \
   -H 'cache-control: no-cache' \
  -d '<?xml version="1.0" encoding="UTF-8"?>
      <md:MetaData xmlns:md="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns="urn:wco:datamodel:WCO:DEC-DMS:2">
      		<md:WCODataModelVersionCode>3.6</md:WCODataModelVersionCode>
      		<md:WCOTypeName>DEC-DMS</md:WCOTypeName>
      		<md:ResponsibleCountryCode>GB</md:ResponsibleCountryCode>
      		<md:ResponsibleAgencyName>Agency ABC</md:ResponsibleAgencyName>
      		<md:AgencyAssignedCustomizationVersionCode>v1.2</md:AgencyAssignedCustomizationVersionCode>
          <Declaration>
         </Declaration>
      </md:MetaData>'
 ```
---

### GET Status Request 
 #### `GET /status-request/mrn/{valid mrn}`

 ##### curl command
 ```
  curl -v -X GET "http://localhost:9820/status-request/mrn/{valid mrn}" \
   -H 'Accept: application/vnd.hmrc.2.0+xml' \
   -H 'Authorization: Bearer {ADD VALID TOKEN}' \
   -H 'Content-Type: application/xml' \
   -H 'X-Badge-Identifier: {Badge Id}' \
   -H 'X-Client-ID: {Valid Client Id}' \
   -H 'cache-control: no-cache' 
 
 ```
---

### Post File Upload 
 #### `POST /file-upload`

 ##### curl command
```
curl -X POST \
  http://localhost:9820/file-upload \
  -H 'Accept: application/vnd.hmrc.1.0+xml' \
  -H 'Authorization: Bearer {ADD VALID TOKEN}' \
  -H 'Content-Type: application/xml; charset=utf-8' \
  -H 'X-Badge-Identifier: {Badge Id}' \
  -H 'X-Client-ID: {Valid Client Id}' \
  -H 'X-EORI-Identifier: {Valid EORI}' \
  -H 'cache-control: no-cache' \
  -d '<hmrc:FileUploadRequest xmlns:hmrc="hmrc:fileupload">
  <hmrc:DeclarationID>123</hmrc:DeclarationID>
  <hmrc:FileGroupSize>2</hmrc:FileGroupSize>
  <hmrc:Files>
    <!--1 or more repetitions:-->
    <hmrc:File>
      <hmrc:FileSequenceNo>1</hmrc:FileSequenceNo>
      <hmrc:DocumentType>"File2"</hmrc:DocumentType>
    </hmrc:File>
     <hmrc:File>
      <hmrc:FileSequenceNo>2</hmrc:FileSequenceNo>
      <hmrc:DocumentType>"File3", File4"</hmrc:DocumentType>
    </hmrc:File>
  </hmrc:Files>
</hmrc:FileUploadRequest>'
 
```
---
