# Customs Declarations

This service offers a general interface for custom declarations, where those declarations are represented using the full DMS implementation of the WCO declarations XML.

This is as apposed to the, as yet undefined, operation specific customs services which will accept requests with more restricted operation specific payloads.

The objective of the General Customs Service - Declaration POST API is as below:

1. Receive a request from a declarant wishing to make a declaration via the generic customs service
2. Validate the request payload conforms to the schema DMS WVO 3.6 schema
3. Pass the request onto CDS WSO2 (for delivery to DMS)
4. Respond to the declarant indicating the success of steps 2 / 3.

It is assumed that the underlying DMS process is asynchronous, and that the only response to the declarant from this API is to indicate the success (or otherwise) of the validation and submission to CDS WSO2 for onward processing by DMS.

# Custom SBT Task for generating ZIP file containing schemas and example messages
There is an SBT task `zipWcoXsds` that generates a ZIP file containing schemas and example messages for each version under `/public/api/conf`
during the packaging phase (so are not generated during normal development). These ZIP files are referenced by the RAML. 
 These references are rendered as HTML links to generated ZIP in the deployed service.

To generate the zip file locally run the following command in command line from project root directory:

    sbt package

# Lookup of `fieldsId` UUID from `api-subscription-fields` service
The `X-Client-ID` header, together with the application context and version are used
 to call the `api-subscription-fields` service to get the unique `fieldsId` UUID to pass on to the backend request.

So there is now a direct dependency on the `api-subscription-fields` service. Note the service to get the `fieldsId` is not currently stubbed. 

## Seeding Data in `api-subscription-fields` for local end to end testing

Make sure the [`api-subscription-fields`](https://github.com/hmrc/api-subscription-fields) service is running on port `9650`. Then run the below curl command.

Please note that version `2.0` is used as an example in the commands given and you should insert the customs declarations api version number which you will call subsequently.

Please note that value `d65f2252-9fcf-4f04-9445-5971021226bb` is used as an example in the commands given and you should insert the UUID value which suits your needs.

    curl -v -X PUT "http://localhost:9650/field/application/d65f2252-9fcf-4f04-9445-5971021226bb/context/customs%2Fdeclarations/version/2.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "https://postman-echo.com/post", "securityToken" : "securityToken" } }'

We then have to manually reset the `fieldId` field to match the id expected by the downstream services. In a mongo command
window paste the following, one after the other.

    use api-subscription-fields

    db.subscriptionFields.update(
        { "clientId" : "d65f2252-9fcf-4f04-9445-5971021226bb", "apiContext" : "customs/declarations", "apiVersion" : "2.0" },
        { $set:
            {"fieldsId" : "d65f2252-9fcf-4f04-9445-5971021226bb"}
        }
    )
    
When you then send a request to `customs-declarations` make sure you have the HTTP header `X-Client-ID` with the value `d65f2252-9fcf-4f04-9445-5971021226bb`

# Multiple wco-dec services - config switching based on ACCEPT header

Two versions of this service are available on the Dev Hub. This is a requirement for External Test allowing for us to have one version (v1)
available publicly and another version (v2) restricted to whitelisted applications.

v1 & v2 have separate configuration sections for the MDG wco-declaration endpoint so the versions may be pointed to different backend services.

Supplying the required ACCEPT header in the request distinguishes which service configuration to use:

| Accept Header | Version |
| ------------- | ------- |
| application/vnd.hmrc.1.0+xml | v1 |
| application/vnd.hmrc.2.0+xml | v2 |

Please note that requests with any other value of `Accept` header are rejected with `406 Not Acceptable` response status.

# Switching service endpoints

Dynamic switching of service endpoints has been implemented for wco declaration connector. To configure dynamic
switching of the endpoint there must be a corresponding section in the application config file
(see example below). This should contain the endpoint config details.


## Example
The service `customs-declarations` has a `default` configuration and a `stub` configuration. Note
that `default` configuration is declared directly inside the `customs-declarations` section.

    Prod {
        ...
        services {
          ...

          wco-declaration {
            host = some.host
            port = 80
            bearer-token = "some_token"
            context = /services/declarationmanagement/1.0.0

            stub {
              host = localhost
              port = 9479
              bearer-token = "some_stub_token"
              context = "/registrations/registerwithid/1.0.0"
            }
          }
          
          v2 {
            wco-declaration {
              host = some.host
              port = 80
              bearer-token = "some_token"
              context = /services/declarationmanagement/1.0.0
      
              stub {
                host = localhost
                port = 9479
                bearer-token = "some_stub_token"
                context = "/registrations/registerwithid/1.0.0"
              }
            }
          }
        }
    }
    
### Switch service configuration for an endpoint

#### REQUEST
    default version (application/vnd.hmrc.1.0+xml):
    curl -X "POST" http://customs-declarations-host/test-only/service/wco-declaration/configuration -H 'content-type: application/json' -d '{ "environment": "stub" }'
    
    version 2 (application/vnd.hmrc.2.0+xml):
    curl -X "POST" http://customs-declarations-host/test-only/service/v2.wco-declaration/configuration -H 'content-type: application/json' -d '{ "environment": "stub" }'

#### RESPONSE

    The service customs-declarations is now configured to use the stub environment

### Switch service configuration to default for an endpoint

#### REQUEST

    curl -X "POST" http://customs-declarations-host/test-only/service/wco-declaration/configuration -H 'content-type: application/json' -d '{ "environment": "default" }'

#### RESPONSE

    The service customs-declarations is now configured to use the default environment

### Get the current configuration for a service

#### REQUEST

    curl -X "GET" http://customs-declarations-host/test-only/service/wco-declaration/configuration

#### RESPONSE

    {
      "service": "wco-declaration",
      "environment": "stub",
      "url": "http://currenturl/customs-declarations"
      "bearerToken": "current token"
    }
# File Upload

The purpose of this endpoint is to receive notifications from the upscan service once a file has been processed


## Supported Endpoints

### Upscan notification

To notify the user once a file has been processed. A HTTP status code of 204 is returned once successful. 

    POST         /uploaded-file-upscan-notifications/decId/:decId/eori/:eori/documentationType/:docType/clientSubscriptionId/:clientSubscriptionId

### Example Payload

    {
        "reference" : "081945ca-952a-4df8-9fc6-2f2679abd7e4",
        "fileStatus" : "READY",
        "uploadDetails": {
          "uploadTimestamp": "2018-04-24T09:30:00Z",
          "checksum": "CHECKSUM"
        },
        "url" : "https://some-url"
    }
    
### Curl

    curl -X "POST" -H "Content-Type: application/json" -d '{ "reference" : "081945ca-952a-4df8-9fc6-2f2679abd7e4", "fileStatus" : "READY", "uploadDetails": { "uploadTimestamp": "2018-04-24T09:30:00Z", "checksum": "CHECKSUM" }, "url" : "https://some-url" }' http://localhost:9000/uploaded-file-upscan-notifications/decId/0f14013a-076b-4dc9-8e5e-45e5706b2b61/eori/054d75b4-491f-4196-873c-37714d37e9ef/documentationType/license/clientSubscriptionId/731667f7-2a2c-4f46-abd8-3245dba74546


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

