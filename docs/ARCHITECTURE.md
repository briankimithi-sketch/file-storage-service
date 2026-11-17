# File Storage Service - Architecture Documentation

## 1. Overview

The File Storage Service is a Spring Boot REST API that allows authenticated users to upload, list, view, download, and delete files.

The application uses:
- Spring Boot for the REST API and application configuration.
- PostgreSQL for storing file metadata.
- The local filesystem for storing the actual file contents.
- Keycloak for obtaining authentication tokens.
- Spring Security OAuth2 Resource Server for validating JWT access tokens.
- Spring Retry and Resilience4j for handling temporary Keycloak failures.
- RabbitMQ components for file event messaging (not currently connected to the upload and delete operations).
- Redis configuration is present, but Spring caching is currently disabled.

## 2. High-Level Architecture

The application follows a layered architecture:

1. Client: sends HTTP requests using Swagger UI, Postman, curl, or another HTTP client.
2. Security layer: checks whether the request is public or requires a valid JWT.
3. Controller layer: receives HTTP requests and returns HTTP responses.
4. Service layer: implements file storage, retrieval, and deletion operations.
5. Repository layer: communicates with PostgreSQL.
6. Storage layer: writes file contents to and reads file contents from the local filesystem.
7. External services: Keycloak handles token requests; RabbitMQ provides messaging infrastructure when event publishing is connected.

## 3. Main Request Flows

### 3.1 Authentication flow

1. The client sends a username and password to POST /public/api/v1/auth-token.
2. TokenController receives the request.
3. KeycloakAuth prepares the authentication request.
4. KeycloakTokenService calls the Keycloak token client through the circuit breaker.
5. KeycloakTokenClient sends the request to Keycloak.
6. If authentication succeeds, the access token is returned to the client.
7. The client sends the token in the Authorization header when accessing protected endpoints.

The protected file endpoints require:

    Authorization: Bearer YOUR_ACCESS_TOKEN

The application uses Spring Security OAuth2 Resource Server to validate JWT access tokens.

### 3.2 File upload flow

1. The client sends a multipart/form-data request to POST /files/upload.
2. FileController receives the uploaded file.
3. The controller delegates the operation to StorageService.
4. FileSystemStorageService validates the filename, file size and allowed extension.
5. The service generates a unique stored filename and creates a date-based directory.
6. The file contents are written to the local filesystem.
7. File metadata is saved through StoredFileRepository in PostgreSQL.
8. The controller returns the upload response, including the stored filename and file URLs.

The file contents and metadata are stored separately:
- Actual file contents: local filesystem.
- File metadata: PostgreSQL.

### 3.3 File listing flow

1. The client sends GET /files with a valid bearer token.
2. FileController requests the file records from StorageService.
3. FileSystemStorageService retrieves records through StoredFileRepository.
4. The service populates the file URLs.
5. The API returns the file records.

The current implementation returns StoredFile entities directly. These records include the filePath property, which may expose internal filesystem locations. A response DTO should be considered to control which fields are returned to clients.

### 3.4 File viewing and downloading flow

1. The client requests GET /files/{filename} to view a file, or GET /files/download/{filename} to download it.
2. FileController looks up the file metadata.
3. StorageService loads the file as a Resource.
4. FileSystemStorageService checks that the resolved path is within the configured storage root and that the file exists.
5. The controller returns the file contents as an HTTP response.

The view endpoint is intended for inline display, while the download endpoint returns the file as an attachment.

### 3.5 File deletion flow

1. The client sends DELETE /files/{filename} with a valid bearer token.
2. FileController delegates the deletion to StorageService.
3. FileSystemStorageService looks up the metadata record.
4. The service validates that the file path is within the configured storage root.
5. The physical file is deleted from the filesystem.
6. The corresponding metadata record is deleted from PostgreSQL.
7. The API returns HTTP 204 No Content when deletion succeeds.

## 4. Authentication and Security

The application has two main categories of endpoints:

- Public endpoints: service root, token endpoint, Swagger UI, OpenAPI documentation and the configured actuator health endpoint. The separate application endpoint GET /health requires authentication.
- Protected endpoints: file operations and other routes not explicitly permitted by SecurityConfig.

File operations require a valid JWT bearer token.

Security responsibilities are divided between:
- TokenController: accepts authentication requests.
- KeycloakAuth: prepares the token request.
- KeycloakTokenService: coordinates Keycloak calls and circuit-breaker protection.
- KeycloakTokenClient: communicates with the Keycloak token endpoint.
- SecurityConfig: configures HTTP security and JWT resource-server validation.

Do not store passwords, access tokens, client secrets, or private keys in source code or documentation.

## 5. Keycloak Retry and Circuit Breaker

The application uses Spring Retry and Resilience4j to improve reliability when communicating with Keycloak.

### 5.1 Spring Retry

KeycloakTokenClient uses retry handling for selected temporary failures, including:
- ResourceAccessException.
- HttpServerErrorException.

The configured defaults are:
- Maximum attempts: 3.
- Initial delay: 1,000 milliseconds.
- Backoff multiplier: 2.
- Maximum delay: 4,000 milliseconds.

The retry mechanism uses increasing delays between attempts. The recovery methods handle exhausted retry attempts and convert relevant failures into application authentication exceptions.

HTTP client errors such as 4xx responses are not included in the configured retry conditions.

### 5.2 Circuit Breaker

KeycloakTokenService calls the Keycloak client through a Resilience4j circuit breaker.

The default configuration is:
- Sliding window size: 10 calls.
- Minimum number of calls: 5.
- Failure-rate threshold: 50%.
- Open-state wait duration: 10,000 milliseconds.
- Permitted calls in half-open state: 3.
- Automatic transition from open to half-open: enabled.

The circuit breaker has three main states:

- CLOSED: requests are allowed normally.
- OPEN: requests are rejected temporarily to avoid repeatedly calling an unhealthy service.
- HALF_OPEN: a limited number of requests are allowed to test whether the external service has recovered.

HTTP client errors are ignored by the circuit-breaker failure calculation. AuthenticationFailedException is configured as a recorded failure.

The configuration also registers Micrometer gauges for failure rate, buffered calls, failed calls, rejected calls and circuit-breaker state.

## 6. File Storage Design

The application stores files in a directory configured by filestorage.root. The default directory is uploads.

Files are organized into date-based directories:

    uploads/YYYY/MM/DD/

The service currently allows these extensions:
- TXT
- PDF
- JPG
- PNG

The maximum upload size is 5 MB.

The service generates a timestamp-based stored filename to reduce filename collisions. It also normalizes file paths and checks that resolved paths remain within the configured storage root.

JPEG images may be compressed, and PNG images may be re-encoded during storage.

Important implementation note: the current extension validation should not be treated as complete content validation. Production deployments should also validate actual file content and MIME type.

The current StoredFile.originalName property contains the generated stored filename. The naming of this property could be improved in a future change to make the distinction between the client's original filename and the stored filename clearer.

## 7. Database and Filesystem Responsibilities

PostgreSQL stores file metadata through StoredFileRepository.

The local filesystem stores the actual file contents.

This separation means that a database record alone does not guarantee that the physical file still exists. The application checks the file when loading it.

The storage directory must be persistent and backed up. Removing or replacing the directory can make database records refer to missing files.

A production deployment should also consider how database records and physical files are kept consistent if an upload or deletion fails partway through an operation.

## 8. RabbitMQ Event Components

The project contains RabbitMQ configuration and event-related classes:

- RabbitMQConfig declares the topic exchange, queues and bindings.
- FileEventProducer contains methods for publishing upload and delete events.
- FileEventListener contains listeners for the configured queues.
- FileEvent represents the event data.

The configured exchange is file.events.exchange. The routing keys are file.uploaded and file.deleted.

The configured queues include file.upload.queue and file.delete.queue.

Current implementation limitation: the upload and delete service operations do not currently call FileEventProducer. The event producer and listener components therefore exist, but end-to-end file event publishing is not currently integrated. The listener auto-startup setting is also disabled in the current application properties.

Do not assume that successful file uploads or deletions currently generate RabbitMQ events.

## 9. Redis and Caching

The project contains cache annotations and Redis connection configuration.

However, the current application configuration sets spring.cache.type=none. Therefore, Spring caching is disabled in the current configuration, and the presence of cache annotations does not mean Redis caching is active.

Redis caching would need to be deliberately enabled and configured before it can be described as an active runtime feature.

## 10. Main Package Responsibilities

| Package | Responsibility |
|---|---|
| config | Security, retry, circuit breaker, RabbitMQ and web configuration |
| controllers | REST API endpoints |
| dto | Request and response data transfer objects |
| entities | Database entity definitions |
| events | File event data |
| exceptions | Application exceptions and global exception handling |
| listeners | RabbitMQ event listeners |
| repositories | Database access |
| services | File storage, retry and event publishing logic |
| utils | Keycloak authentication and token handling |

The main application class is FileStorageServiceApplication. It enables Spring Boot component scanning, Spring caching annotations and Spring Retry.

## 11. Configuration and Deployment Notes

Important settings are configured through application properties and environment variables.

Examples include:
- Server port: 8081.
- Database host, port, name, username and password.
- File storage root directory.
- Maximum file upload size.
- Keycloak connection and retry settings.
- Circuit-breaker settings.
- RabbitMQ connection settings.
- CORS allowed origins.

Use environment variables or a secure configuration system for environment-specific values and secrets.

The Dockerfile currently declares EXPOSE 8080, while the Spring Boot application is configured to listen on port 8081. Docker EXPOSE does not change the application's listening port. The Docker configuration and deployment port mapping should be aligned before deployment.

The storage directory must be mounted on persistent storage when running in a container.

## 12. Testing and Verification

The automated test suite covers file storage, controllers, authentication, security, retry handling and Keycloak integration.

The project was verified on 17 November 2026 using:

    mvn clean verify

The recorded result was:
- 50 tests executed.
- 0 failures.
- 0 errors.
- 0 skipped.
- BUILD SUCCESS.

This confirms that the automated test suite passed in the environment where the command was run. It does not replace testing against the actual deployment environment and external services.

## 13. Known Improvements to Consider

The following improvements should be considered before production use:

1. Return file metadata through response DTOs instead of exposing database entities and internal file paths.
2. Validate file contents and MIME types in addition to filename extensions.
3. Integrate FileEventProducer with successful upload and delete operations if event publishing is required.
4. Enable and configure RabbitMQ listener startup when event consumption is required.
5. Decide whether Redis caching is required and configure it accordingly.
6. Align the Docker exposed port and deployment port mapping with the application's configured port.
7. Ensure file storage is persistent, backed up and monitored.
8. Review endpoint authorization and CORS settings for each deployment environment.
9. Review file and database consistency when filesystem operations or database operations fail.

## 14. Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Application programming language |
| Spring Boot 3.2 | Application framework |
| Spring Web | REST API development |
| Spring Security | Application security |
| OAuth2 Resource Server | JWT validation |
| Keycloak | Authentication and token issuance |
| PostgreSQL | File metadata storage |
| Spring Data JPA | Database access |
| Spring Retry | Retry handling |
| Resilience4j | Circuit-breaker protection |
| RabbitMQ | Messaging infrastructure |
| Redis | Configured caching infrastructure; caching currently disabled |
| Maven | Build and dependency management |

## 15. API Error Handling

The application uses HTTP status codes to communicate errors.

| HTTP status | Meaning |
|---|---|
| 400 | Invalid file type, empty upload or invalid request |
| 401 | Authentication failure or missing/invalid authentication |
| 404 | Requested file or resource not found |
| 413 | Uploaded file exceeds the configured size limit |
| 500 | Unexpected application or file I/O error |
| 204 | Successful file deletion with no response body |

Clients should use the HTTP status code and response body to identify and handle errors.

## 16. API Security and CORS

Spring Security protects file operations using bearer JWT authentication.

WebConfig defines allowed frontend origins and HTTP methods. The allowed origins should be reviewed and updated for each deployment environment.

Production deployments should use HTTPS, restrict CORS to trusted frontend origins, protect Keycloak credentials and avoid exposing internal filesystem paths in API responses.

## 17. Related Documentation

- [Project README](../README.md)
- [API Documentation](API-DOCUMENTATION.md)
- [User Guide](USER-GUIDE.md)
- [Installation Guide](INSTALLATION-GUIDE.md)
