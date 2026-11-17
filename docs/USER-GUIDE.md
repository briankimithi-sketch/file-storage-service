# File Storage Service – User Guide

## 1. Introduction

This guide explains how to use the File Storage Service to upload, manage, view and download documents.

The service provides a REST API. Users can interact with it through Swagger UI, Postman or command-line tools such as curl.

## 2. Before You Begin

You need:

- Access to a running File Storage Service.
- A valid Keycloak username and password.
- A supported file to upload.
- Permission to access the service.

Supported file types:

- PDF
- TXT
- JPG
- PNG

Maximum upload size: 5 MB.

## 3. Opening the API Interface

1. Start the File Storage Service.
2. Open a web browser.
3. Navigate to:

       http://localhost:8081/swagger-ui/index.html

4. Swagger UI displays the available API operations.
5. Expand an endpoint to see its details.
6. Select Try it out to test an operation.
7. Enter the required information and execute the request.

For a deployed environment, replace localhost with the appropriate service address.

## 4. Signing In and Obtaining an Access Token

The service uses Keycloak authentication.

1. Locate the POST `/public/api/v1/auth-token` endpoint.
2. Expand the endpoint.
3. Select Try it out.
4. Enter your Keycloak username and password in the request body.

Example:

       {
         "username": "your-username",
         "password": "your-password"
       }

5. Select Execute.
6. If authentication succeeds, the response contains an `accessToken`.
7. Copy the access token.

Keep this token private. It grants access to protected service operations.

## 5. Authorizing Requests

Protected operations require a bearer token.

When using Swagger UI:

1. Find the Authorize option if it is available.
2. Enter the bearer token in the format required by the interface.
3. Confirm authorization.
4. Execute the protected operations.

If the Swagger UI does not display an Authorize option, use Postman or curl and manually add this HTTP header:

       Authorization: Bearer YOUR_ACCESS_TOKEN

Do not include your username or password in this header.

## 6. Uploading a Document

1. Obtain a valid access token.
2. Open POST `/files/upload`.
3. Select Try it out.
4. Choose a file using the file input.
5. Execute the request.
6. Review the response.

The upload request must use the multipart field named `file`.

A successful response provides:

- Generated filename.
- File size.
- Download URL.
- View URL.

Keep the generated filename. You will need it to view, download or delete the document.

If the upload fails, check that:
- The file is not empty.
- The extension is supported.
- The file does not exceed 5 MB.
- You supplied a valid access token.

## 7. Viewing Uploaded Documents

1. Locate the GET `/files` endpoint.
2. Execute it using your access token.
3. Review the returned file records.
4. Copy the filename of the document you want.
5. Open GET `/files/{filename}`.
6. Enter the filename and execute the request.

The endpoint returns the file for inline viewing where the browser supports its content type.

## 8. Downloading Documents

1. Obtain the filename from the upload response or file listing.
2. Open GET `/files/download/{filename}`.
3. Enter the filename.
4. Execute the request.
5. Save the returned file.

The download endpoint returns the file as an attachment.

## 9. Listing Documents

1. Open GET `/files`.
2. Ensure you have authorized the request.
3. Execute the operation.
4. Review the returned list of stored files.

The response includes file metadata such as filename, content type, size and creation date.

## 10. Deleting a Document

**Warning:** Deletion removes the stored file. Only delete a document when you are sure it is no longer required.

1. Find the document's generated filename.
2. Open DELETE `/files/{filename}`.
3. Enter the filename.
4. Execute the request.
5. A successful deletion returns HTTP 204 No Content.

## 11. Using Postman

You can also use Postman to interact with the API.

### Obtain a token

1. Create a new POST request.
2. Enter:

       http://localhost:8081/public/api/v1/auth-token

3. Select Body, then raw, then JSON.
4. Enter your Keycloak username and password.
5. Send the request.
6. Copy the returned `accessToken`.

### Configure authenticated requests

For each protected request:

1. Open the Authorization tab.
2. Select Bearer Token.
3. Paste the access token.
4. Enter the required endpoint and HTTP method.
5. Send the request.

### Upload using Postman

1. Create a POST request to `/files/upload`.
2. Open Body.
3. Select form-data.
4. Add a key named `file`.
5. Change the key type to File.
6. Select the document.
7. Send the request.

Postman will generate the multipart content type automatically.

## 12. Common Problems and Solutions

| Problem | Possible solution |
|---|---|
| 401 Unauthorized | Obtain a new token and check the Authorization header |
| 403 Forbidden | Confirm that your account has permission to access the operation |
| 400 Bad Request | Check the request format and file type |
| 404 Not Found | Check the filename and endpoint |
| 413 Payload Too Large | Use a file within the 5 MB limit |
| 500 Internal Server Error | Contact the service administrator |

## 13. Security Guidelines

- Never share your Keycloak password.
- Never share your access token.
- Do not upload confidential documents unless authorized.
- Do not expose document URLs or access credentials publicly.
- Use the approved service URL for your environment.
- Report unexpected access or upload problems to the service administrator.

## 14. Support

For technical assistance, provide the service administrator with:

- The endpoint being accessed.
- The HTTP method.
- The HTTP response status.
- The error message.
- The approximate time the issue occurred.

Do not send passwords, private keys or access tokens when reporting an issue.
