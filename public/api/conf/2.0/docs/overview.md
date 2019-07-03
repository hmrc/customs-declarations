This API allows the submission of any UCC declarations for asynchronous processing by the customs declarations service, to enable a user to have their declarations accepted by means of a single API.

The API takes any UCC declaration payload. It also allows for the submission of a cancellation request (pre-clearance) and provides a method of securely uploading additional documentation to support a declaration submission.

The file-upload endpoint is used to initiate a file upload as part of the declaration submission process. An example document you may be requested to upload could be a paper copy of a licence. This endpoint is used to initiate a file upload where a signed URL is returned by the endpoint to be used in the file upload workflow.
