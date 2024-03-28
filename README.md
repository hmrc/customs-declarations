# Customs Declarations

This service offers a general interface for custom declarations, where those declarations are represented using the WCO declarations schema. In addition to submitting declarations, the Customs Declarations service allows you to amend and cancel them. It also provides the ability to upload supporting documents and make arrival notifications.

The objective of the Customs Declarations Service API is:

1. Receive a request from a declarant wishing to make a declaration
2. Validate the request payload conforms to the schema DMS WCO 3.6 schema
3. Pass the request onto CDS backend
4. Respond to the declarant indicating the success of steps 2 / 3.

As the notification process is asynchronous the only response to the declarant from this API is to indicate the success (or otherwise) of the validation and submission to the CDS backend.

## Development Setup
- This microservice requires mongoDB 4.+
- Run locally: `sbt run` which runs on port `9820` by default
- Run with test endpoints: `sbt 'run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes'`

##  Service Manager Profiles
The Customs Declarations service can be run locally from Service Manager, using the following profiles:

| Profile Details                       | Command                                                           | Description                                                    |
|---------------------------------------|:------------------------------------------------------------------|----------------------------------------------------------------|
| CUSTOMS_DECLARATION_ALL               | sm2 --start CUSTOMS_DECLARATION_ALL                               | To run all CDS applications.                                   |
| CUSTOMS_INVENTORY_LINKING_EXPORTS_ALL | sm2 --start CUSTOMS_INVENTORY_LINKING_EXPORTS_ALL                 | To run all CDS Inventory Linking Exports related applications. |
| CUSTOMS_INVENTORY_LINKING_IMPORTS_ALL | sm2 --start CUSTOMS_INVENTORY_LINKING_IMPORTS_ALL                 | To run all CDS Inventory Linking Imports related applications. |

## Run Tests
- Run Unit Tests: `sbt test`
- Run Integration Tests: `sbt IntegrationTest/test`
- Run Unit and Integration Tests: `sbt test IntegrationTest/test`
- Run Unit and Integration Tests with coverage report: `./run_all_tests.sh`<br/> which runs `clean scalastyle coverage test it:test coverageReport dependencyUpdates"`

### Acceptance Tests
To run the CDS acceptance tests, see [here](https://github.com/hmrc/customs-automation-test).

### Performance Tests
To run performance tests, see [here](https://github.com/hmrc/customs-declaration-performance-test).


## API documentation
For Customs Declarations API documentation, see [here](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/customs-declarations).

### Customs Declarations specific routes
| Path - internal routes prefixed by `/customs-declarations`                                            | Supported Methods | Description                                                           |
|-------------------------------------------------------------------------------------------------------|:-----------------:|-----------------------------------------------------------------------|
| `/customs-declarations/`                                                                              |       POST        | Endpoint to submit a declaration.                                     |
| `/customs-declarations/cancellation-requests`                                                         |       POST        | Endpoint to cancel a declaration.                                     |
| `/customs-declarations/amend`                                                                         |       POST        | Endpoint to amend a declaration.                                      |
| `/customs-declarations/file-upload  `                                                                 |       POST        | Endpoint to submit a file upload.                                     |
| `/customs-declarations/uploaded-file-upscan-notifications/clientSubscriptionId/:clientSubscriptionId` |       POST        | Endpoint to manage file upload upscan notifications.                  |
| `/customs-declarations/file-transmission-notify/clientSubscriptionId/:clientSubscriptionId`           |       POST        | Endpoint to submit file upload notifications to customs notification. |
| `/customs-declarations/arrival-notification`                                                          |       POST        | Endpoint to submit an arrival notification declaration.               |
| `/customs-declarations/status-request/mrn/:mrn`                                                       |        GET        | Endpoint to retrieve the status of a declaration.                     |

### Test-only specific routes
| Path                         | Supported Methods | Description                                  |
|------------------------------|:-----------------:|----------------------------------------------|
| `/file-upload/test-only/all` |      DELETE       | Endpoint to delete all file upload metadata. |


## Calling /file-upload

A request to the /file-upload endpoint can contain successRedirect and errorRedirect URLs. These are non-mandatory.
If both a success and error redirect are included then upscan v2 will be called, otherwise v1.

## Useful CURL commands for local testing
[link to curl commands](docs/curl-commands.md)
# Custom SBT Task for generating ZIP file containing schemas and example messages
There is an SBT task `zipWcoXsds` that generates a ZIP file containing schemas and example messages for each version under `/public/api/conf`
during the packaging phase (so are not generated during normal development). These ZIP files are referenced by the YAML. 
 These references are rendered as HTML links to generated ZIP in the deployed service.

To generate the zip file locally run the following command in command line from project root directory:

    sbt package

## Lookup of `fieldsId` UUID from `api-subscription-fields` service
The `X-Client-ID` header, together with the application context and version are used
 to call the `api-subscription-fields` service to get the unique `fieldsId` UUID to pass on to the backend request.

Note the service to get the `fieldsId` is not currently stubbed. 

## Seeding Data in `api-subscription-fields` for local end-to-end testing

Make sure the [`api-subscription-fields`](https://github.com/hmrc/api-subscription-fields) service is running on port `9650`. Then run the below curl command.

Please note that version `2.0` is used as an example in the commands given, and you should insert the customs declarations api version number which you will call subsequently.

Please note that value `d65f2252-9fcf-4f04-9445-5971021226bb` is used as an example in the commands given, and you should insert the UUID value which suits your needs.

    curl -v -X PUT "http://localhost:9650/field/application/d65f2252-9fcf-4f04-9445-5971021226bb/context/customs%2Fdeclarations/version/2.0" -H "Cache-Control: no-cache" -H "Content-Type: application/json" -d '{ "fields" : { "callbackUrl" : "https://postman-echo.com/post", "securityToken" : "securityToken", "authenticatedEori": "ABC123" } }'

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

Three versions of this service are available on the Developer Hub.

Each have separate configuration sections for the MDG wco-declaration endpoint so the versions may be pointed to different backend services.

Supplying the required ACCEPT header in the request distinguishes which service configuration to use:

| Accept Header | Version |
| ------------- | ------- |
| application/vnd.hmrc.1.0+xml | v1 |
| application/vnd.hmrc.2.0+xml | v2 |
| application/vnd.hmrc.3.0+xml | v3 |

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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

