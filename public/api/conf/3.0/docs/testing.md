You can use the HMRC Developer Sandbox to test the Customs Declarations API. The Sandbox is an enhanced testing service that functions as a simulator of HMRC’s production environment.
To use the Customs Declaration API you will need to create your own test user. To create your own test user:
- Use the [Create Test User API](/api-documentation/docs/api/service/api-platform-test-user/1.0#_create-a-test-user-which-is-an-organisation_post_accordion)
- Ensure under ‘Body’ “customs-services” is requested 
- The first time you call this API the username and password log in box should appear (select do not have credentials, follow the steps & enter the username/password provided). 
- If successful this will prompt the grant authority to your application and select approve. If correct, you should see a 200 response showing some test data including a username, password and EORI (Please Ignore this EORI, only use the EORI that has been assigned to you for TT).
- After completing the above, if you call the Customs Declaration API, the same process should occur whereby it will ask for a username and password (Please enter the Username and Password that is in the test data generated from the step above when calling the create test user API)
Please Note: 443 is the only allowed port, which means port 443 will need to be used in the callback URL.