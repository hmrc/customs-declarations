# Customs Declarations Curl Commmands
---
### Endpoints Summary

| Path                                                                                                                            |  Method  | Description                                |
|---------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------|
| [`/`](#user-content-post-customs-declaration)                                                                                   |   `POST` |    Allows submission of a Customs Declaration |
| [`/arrival-notification`](#user-content-post-arrival-notification)                                                              |   `POST` |    Allows submission of a Customs Arrival Notification Declaration | 
| [`/cancellation-requests`](#user-content-post-cancellation-requests)                                                            |   `POST` |    Allows submission of a cancellation request |
| [`/clearance`](#user-content-post-clearance)                                                                                    |   `POST` |    Allows submission of a Customs Clearance Declaration |

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