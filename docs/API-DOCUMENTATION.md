# File Storage Service – API Documentation

## 1. Overview

The File Storage Service is a REST API that allows authenticated users to upload, list, view, download and delete files.

Keycloak handles authentication. Protected endpoints require a valid JWT access token.

## 2. Base URL

Local development:

    http://localhost:8081

Replace this with the deployed service URL in other environments.

## 3. Authentication

### Obtain an access token

**Method:** POST

**Endpoint:** `/public/api/v1/auth-token`

Authentication is not required for this endpoint.

Request body:

    {
      "username": "your-username",
      "password": "your-password"
    }

Example:

    curl -X POST http://localhost:8081/public/api/v1/auth-token \
      -H "Content-Type: application/json" \
      -d '{"username":"your-username","password":"your-password"}'

Successful response:

    {
      "accessToken": "your-jwt-access-token"
    }

### Using the access token

Include this header in protected requests:

    Authorization: Bearer YOUR_ACCESS_TOKEN

Never share your password or access token.

## 4. API Endpoints

### 4.1 Service information

**Method:** GET

**Endpoint:** `/`

**Authentication:** Public.

**Description:** Confirms that the service is running.

Example:

    curl http://localhost:8081/

Response:

    {
      "service": "file-storage-service",
      "status": "running"
    }

### 4.2 Application health

**Method:** GET

**Endpoint:** `/health`

**Authentication:** Required.

**Description:** Returns the application's health status.

Example:

    curl http://localhost:8081/health \
      -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

Response:

    {
      "status": "UP"
    }

Note: `/actuator/health` is a separate endpoint and is publicly accessible under the current security configuration.

### 4.3 Upload a file

**Method:** POST

**Endpoint:** `/files/upload`

**Authentication:** Required.

**Content type:** `multipart/form-data`

**Description:** Uploads a file to the service.

The multipart field name must be `file`.

Supported extensions:
- TXT
- PDF
- JPG
- PNG

Maximum configured upload size: 5 MB.

Example:

    curl -X POST http://localhost:8081/files/upload \
      -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
      -F "file=@/path/to/document.pdf"

Replace the file path with the location of your actual file.

The response contains:
- `originalName`: generated filename used by the service.
- `size`: stored file size.
- `downloadUrl`: download link.
- `viewUrl`: viewing link.

Use the returned filename when accessing the file later.

### 4.4 List stored files

**Method:** GET

**Endpoint:** `/files`

**Authentication:** Required.

**Description:** Returns stored file records.

Example:

    curl http://localhost:8081/files \
      -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

The response is a JSON array containing fields such as:

| Field | Description |
|---|---|
| `id` | Database record identifier |
| `originalName` | Stored filename |
| `filePath` | File storage path |
| `contentType` | File content type |
| `size` | File size |
| `createdOn` | Record creation date |
| `downloadUrl` | Download link |
| `viewUrl` | View link |

**Security note:** The current endpoint returns the entity directly, including `filePath`. Consider using a response DTO to prevent internal filesystem paths from being exposed to API consumers.

### 4.5 Download a file

**Method:** GET

**Endpoint:** `/files/download/{filename}`

**Authentication:** Required.

**Description:** Downloads a stored file as an attachment.

Example:

    curl -X GET "http://localhost:8081/files/download/YOUR_FILENAME" \
      -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
      -o downloaded-file

The `-o` option saves the downloaded content locally.

### 4.6 View a file

**Method:** GET

**Endpoint:** `/files/{filename}`

**Authentication:** Required.

**Description:** Returns a file for inline viewing where the browser supports its content type.

Example:

    curl -X GET "http://localhost:8081/files/YOUR_FILENAME" \
      -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
      -o viewed-file

### 4.7 Delete a file

**Method:** DELETE

**Endpoint:** `/files/{filename}`

**Authentication:** Required.

**Description:** Deletes the specified stored file.

Example:

    curl -X DELETE "http://localhost:8081/files/YOUR_FILENAME" \
      -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

Successful response:

    HTTP 204 No Content

There is no response body after successful deletion.

## 5. Endpoint Summary

| Method | Endpoint | Authentication | Purpose |
|---|---|---|---|
| GET | `/` | Public | Service information |
| GET | `/health` | Required | Application health |
| POST | `/public/api/v1/auth-token` | Public | Obtain access token |
| POST | `/files/upload` | Required | Upload a file |
| GET | `/files` | Required | List stored files |
| GET | `/files/download/{filename}` | Required | Download a file |
| GET | `/files/{filename}` | Required | View a file |
| DELETE | `/files/{filename}` | Required | Delete a file |

## 6. Common Error Responses

| HTTP status | Meaning | Typical cause |
|---|---|---|
| 400 Bad Request | Invalid request | Empty file, unsupported extension or invalid input |
| 401 Unauthorized | Authentication failed | Missing or invalid credentials or token |
| 403 Forbidden | Access denied | Request is not permitted |
| 404 Not Found | Resource not found | File or endpoint does not exist |
| 413 Payload Too Large | Upload too large | File exceeds configured limit |
| 500 Internal Server Error | Server error | Unexpected application or file I/O failure |

Example:

    {
      "error": "File not found with filename: example.pdf"
    }

The exact error response depends on the failure.

## 7. File Storage Behaviour

- Files are stored under the configured storage root directory.
- Files are organized into date-based directories using year, month and day.
- Generated filenames help avoid collisions.
- Empty files are rejected.
- File extensions are checked against the supported list.
- Image files are processed during upload.
- File metadata is stored in PostgreSQL.
- The configured maximum upload size is 5 MB.

## 8. API Documentation Interface

The project includes Springdoc OpenAPI.

When the application is running, try:

Swagger UI:

    http://localhost:8081/swagger-ui/index.html

OpenAPI specification:

    http://localhost:8081/v3/api-docs

Swagger UI provides an interactive interface for exploring and testing API operations.

## 9. Important Notes

- Obtain an access token before calling protected endpoints.
- Include the bearer token in every protected request.
- Use the filename returned by the service when viewing, downloading or deleting a file.
- Do not expose credentials, private keys or access tokens.
- The examples use a local development URL. Update the base URL for other environments.
