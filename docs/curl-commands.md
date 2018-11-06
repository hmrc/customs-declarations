# Customs Declarations Curl Commmands
---
### Endpoints Summary

| Path                                                                                                                            |  Method  | Description                                |
|---------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------|
| [`/`](#user-content-post-customs-declaration)                                                               | `POST`   | Allows submission of a Customs Declaration |
 
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