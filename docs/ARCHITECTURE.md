# File Storage Service – Architecture Documentation

## 1. Purpose

The File Storage Service is a Spring Boot REST API that allows authenticated clients to upload, list, view, download, and delete files.

It stores file contents on the server's filesystem and stores file metadata in a PostgreSQL database. Keycloak is used for authentication, while Spring Security validates JWT access tokens on protected endpoints.

## 2. Technology Stack

- Java 17
- Spring Boot 3.2
- Spring Web
- Spring Security and OAuth2 Resource Server
- Keycloak for authentication
- PostgreSQL for file metadata
- Spring Data JPA and Hibernate
- Spring Retry for retrying selected Keycloak communication failures
- Resilience4j for circuit-breaker protection
- RabbitMQ components for file-event messaging
- Redis configuration is present, but Spring caching is currently disabled
- Maven for dependency management and builds

## 3. High-Level Architecture

The service follows a layered architecture:

1. Client layer – a frontend, Postman, or another HTTP client sends requests.
2. Controller layer – receives HTTP requests and returns HTTP responses.
3. Security layer – checks whether protected requests contain a valid JWT.
4. Service layer – implements file-storage and authentication business logic.
5. Repository layer – communicates with PostgreSQL through Spring Data JPA.
6. Storage layer – reads and writes physical files on the server filesystem.
7. External services – Keycloak handles authentication; RabbitMQ components provide messaging infrastructure.

## 4. Main Package Responsibilities

| Package | Responsibility |
| --- | --- |
| config | Security, web, retry, circuit-breaker, and RabbitMQ configuration |
| controllers | REST API endpoints |
| dto | Request and response data-transfer objects |
| entities | Database entities such as StoredFile |
| events | File event data structures |
| exceptions | Application exceptions and global error handling |
| listeners | RabbitMQ event listeners |
| repositories | Database access |
| services | File storage, event production, and supporting business logic |
| utils | Keycloak authentication and token communication |

The main application class is FileStorageServiceApplication. It enables Spring Boot component scanning, Spring Retry, and Spring caching annotations.

## 5. File Upload Flow

The normal upload process is:

1. The client sends a multipart HTTP POST request to `/files/upload`.
2. Spring Security checks the client's bearer token.
3. FileController receives the uploaded file.
4. The controller delegates the operation to StorageService.
5. FileSystemStorageService validates the filename, file contents, and supported extension.
6. The service generates a unique stored filename and a date-based directory.
7. The file is written to the server filesystem.
8. StoredFile metadata is saved through StoredFileRepository in PostgreSQL.
9. The API returns information about the stored file, including its size and generated download/view URLs.

The supported extensions in the current implementation are TXT, PDF, JPG, and PNG. The configured maximum upload size is 5 MB.

Image files may be processed before storage. PNG images are re-encoded, while JPEG images are compressed using a quality setting of 0.75.

## 6. File Download and Viewing Flow

1. The client sends a request to the download or view endpoint.
2. Spring Security validates the bearer token.
3. FileController looks up the requested file metadata.
4. StoredFileRepository retrieves the database record.
5. FileSystemStorageService checks that the physical file exists and is inside the configured storage root.
6. The service returns the file as a resource.
7. The controller sends the file to the client.

The download endpoint returns the file as an attachment. The view endpoint returns it for inline viewing where supported by the client.

## 7. File Deletion Flow

1. The client sends an authenticated DELETE request.
2. FileController delegates the operation to the storage service.
3. The service searches PostgreSQL for the requested file record.
4. It validates the stored path against the configured storage root.
5. It deletes the physical file if it exists.
6. It deletes the corresponding database record.

The current implementation performs filesystem deletion before deleting the database record. A failure between these operations can leave the database and filesystem out of sync; this should be considered when improving reliability.

## 8. Data Storage

### PostgreSQL

PostgreSQL stores file metadata in the `stored_files` table.

The StoredFile entity includes:

- id
- originalName (currently used to hold the generated stored filename)
- filePath
- contentType
- size
- createdOn
- transient downloadUrl and viewUrl values

The database stores information about files; the actual file contents are stored on the filesystem.

### Filesystem

The default storage directory is `uploads`, resolved relative to the application's working directory. The service creates date-based subdirectories using the year, month, and day.

The service normalizes paths and checks that resolved paths remain inside the configured storage root. This helps prevent path-traversal attacks.

**Deployment consideration:** The storage directory must be persistent and backed up. Container filesystems can be temporary, so deployments should mount a persistent volume for uploaded files.

## 9. Authentication and Authorization

Keycloak is the external identity provider.

The authentication flow is:

1. A client submits username and password to `POST /public/api/v1/auth-token`.
2. TokenController delegates authentication to the Keycloak authentication components.
3. KeycloakAuth requests an access token through KeycloakTokenService and KeycloakTokenClient.
4. The service returns the access token to the client.
5. The client sends the token in subsequent protected requests using the HTTP header:

   Authorization: Bearer <access-token>

6. Spring Security's OAuth2 Resource Server validates the JWT before allowing access to protected endpoints.

The file endpoints require authentication. The root endpoint, token endpoint, Swagger UI, OpenAPI documentation, and configured actuator health endpoint are permitted without authentication.

The `/health` application endpoint is not explicitly permitted in the current security configuration and therefore requires authentication.

Never commit real passwords, client secrets, access tokens, private keys, or production service credentials to source control.

## 10. Keycloak Retry Mechanism

The service uses Spring Retry to handle selected temporary failures when communicating with Keycloak.

The KeycloakTokenClient retries:

- ResourceAccessException
- HttpServerErrorException

The configured defaults are:

- Maximum attempts: 3
- Initial delay: 1,000 milliseconds
- Exponential multiplier: 2
- Maximum delay: 4,000 milliseconds

This means the service can retry selected network and server-side failures rather than immediately failing the authentication request.

HTTP client errors, such as invalid credentials or other 4xx responses, are not included in the retry conditions.

Recovery methods handle failures after the retry attempts have been exhausted and convert relevant failures into application authentication exceptions.

## 11. Keycloak Circuit Breaker

The Keycloak authentication service also uses Resilience4j CircuitBreaker.

The circuit breaker protects the application when Keycloak is repeatedly unavailable.

Its default configuration is:

| Setting | Default |
| --- | --- |
| Sliding window size | 10 calls |
| Minimum calls before evaluation | 5 calls |
| Failure rate threshold | 50% |
| Open-state wait duration | 10 seconds |
| Permitted calls in half-open state | 3 |

The circuit breaker has three states:

- CLOSED: Requests are allowed normally and failures are monitored.
- OPEN: Requests are rejected temporarily to avoid repeatedly calling an unhealthy Keycloak service.
- HALF_OPEN: A limited number of requests are allowed to test whether Keycloak has recovered.

The implementation also registers Micrometer gauges for failure rate, buffered calls, failed calls, rejected calls, and circuit-breaker state.

## 12. RabbitMQ and File Events

The project contains RabbitMQ configuration, a FileEventProducer, a FileEvent data structure, and a FileEventListener.

The configured messaging components include:

- Topic exchange: `file.events.exchange`
- Upload routing key: `file.uploaded`
- Delete routing key: `file.deleted`
- Upload queue: `file.upload.queue`
- Delete queue: `file.delete.queue`

The producer can construct and publish upload and delete events. The listener currently logs received events.

**Current implementation status:** The producer methods are not currently called by the upload or delete service flow. The RabbitMQ listener is also configured not to start automatically. Therefore, the presence of these classes does not mean that end-to-end file-event processing is currently active.

To activate this functionality, the producer must be integrated into the relevant file operations, and listener startup and broker configuration must be reviewed and tested.

## 13. Caching and Redis

The application enables Spring caching annotations, and the file-storage service contains cache annotations for file listing and cache eviction after changes.

However, the current application configuration sets:

`spring.cache.type=none`

Therefore, Spring caching is disabled at runtime. Redis configuration may be present, but it should not be described as an active file cache in the current deployment.

If caching is required, the cache provider and Redis connection settings must be configured and verified.

## 14. Error Handling

GlobalExceptionHandler centralizes API exception handling.

The service handles common errors including:

- File not found – HTTP 404
- Unsupported file type – HTTP 400
- Invalid or empty upload – HTTP 400
- Authentication failure – HTTP 401
- Upload exceeds configured size – HTTP 413
- File I/O failure – HTTP 500
- Unexpected application error – HTTP 500

Clients should use the HTTP status code and response body to identify and handle errors.

## 15. API Security and CORS

Spring Security protects file operations using bearer JWT authentication.

WebConfig defines allowed frontend origins and HTTP methods. The allowed origins should be reviewed and updated for each deployment environment.

Production deployments should use HTTPS, restrict CORS to trusted frontend origins, protect Keycloak credentials, and avoid exposing internal filesystem paths in API responses.

## 16. Configuration and Deployment Notes

Important configuration areas include:

- Server port and bind address
- PostgreSQL connection details
- File storage root directory
- Maximum multipart upload size
- Keycloak issuer and token endpoint
- Keycloak retry and circuit-breaker settings
- RabbitMQ connection and listener settings
- Redis/cache settings

Use environment variables or a secure configuration provider for environment-specific values. Do not put real credentials in documentation or source control.

The application configuration uses port 8081. The current Dockerfile declares `EXPOSE 8080`; Docker's EXPOSE instruction does not change the application's listening port. Review the Dockerfile and container port mapping so they agree with the configured server port.

## 17. Known Improvements

The following improvements are recommended:

1. Return a response DTO from the file-list endpoint rather than exposing the entity's absolute filesystem path.
2. Consider renaming the StoredFile `originalName` field or introducing a separate `storedFilename` field to make its current meaning clear.
3. Validate actual file content and MIME type in addition to checking the filename extension.
4. Integrate RabbitMQ event publishing into successful upload and delete operations if event-driven processing is required.
5. Enable and test RabbitMQ listeners only when the required broker configuration is available.
6. Decide whether Redis caching is required; if so, configure and test a real cache provider.
7. Review the security rules for `/health`.
8. Align the Dockerfile's declared port and deployment port mapping with the application's configured port.
9. Add operational monitoring and persistent backups for the database and uploaded files.

## 18. Related Documentation

- [Project README](../README.md)
- [API Documentation](API-DOCUMENTATION.md)
- [User Guide](USER-GUIDE.md)
- [Installation Guide](INSTALLATION-GUIDE.md)
