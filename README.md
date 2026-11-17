# File Storage Service

A RESTful file storage application built with Java 17 and Spring Boot 3.2.0. It provides authenticated file upload, listing, viewing, downloading and deletion, with PostgreSQL metadata storage and Keycloak-based authentication.

## 1. Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Application runtime |
| Spring Boot 3.2.0 | Application framework |
| Maven | Build and dependency management |
| Spring Web | REST API endpoints |
| Spring Security | API security |
| Keycloak | Identity and access management |
| Spring OAuth2 Resource Server | JWT validation |
| Spring Data JPA / Hibernate | Database access |
| PostgreSQL | File metadata persistence |
| Redis | Caching support |
| RabbitMQ | Messaging and file events |
| Spring Retry | Retrying temporary failures |
| Resilience4j | Circuit breaker |
| Springdoc OpenAPI | API documentation interface |
| JUnit 5 | Automated testing |

## 2. Main Features

- Upload supported files: TXT, PDF, JPG and PNG.
- Maximum configured upload size: 5 MB.
- Generate unique filenames to avoid collisions.
- Store files in date-based directories.
- Compress supported images during upload.
- List stored file metadata.
- View and download stored files.
- Delete stored files.
- Authenticate users through Keycloak.
- Protect file endpoints using JWT bearer tokens.
- Apply retry and circuit-breaker protection to Keycloak communication.

## 3. Project Structure

The main application code is under `src/main/java/com/abcbank/filestorage/`.

| Package | Responsibility |
|---|---|
| `config` | Security, retry, messaging and application configuration |
| `controllers` | REST API endpoints |
| `dto` | Request and response objects |
| `entities` | JPA database entities |
| `events` | File event definitions |
| `exceptions` | Application exceptions and global error handling |
| `listeners` | Message/event listeners |
| `repositories` | Database access |
| `services` | File storage, messaging and supporting services |
| `utils` | Keycloak authentication and token handling |

## 4. Requirements

- Java Development Kit (JDK) 17
- Maven 3.8 or later, or the included Maven wrapper
- PostgreSQL
- Keycloak configured for the FileStorage realm
- Redis and RabbitMQ where the corresponding integrations are enabled

## 5. Configuration

Application configuration is maintained in:

- `src/main/resources/application.properties`
- `src/main/resources/application-local.properties` for local development

The application is configured to use port `8081` by default.

Configure environment-specific values outside version control. In particular, database passwords, Keycloak credentials, RabbitMQ passwords and other secrets must not be committed to Git or included in documentation.

Important configuration properties include:

| Property | Purpose |
|---|---|
| `server.port` | HTTP server port |
| `spring.datasource.url` | PostgreSQL connection URL |
| `spring.datasource.username` | Database username |
| `spring.datasource.password` | Database password |
| `filestorage.root` | File storage directory |
| `filestorage.base-url` | Base URL used to construct file links |
| `spring.servlet.multipart.max-file-size` | Maximum individual upload size |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | Keycloak JWT issuer |
| `config.params.keycloak.url` | Keycloak token endpoint |
| `config.params.keycloak.client-id` | Keycloak client identifier |
| `spring.data.redis.host` | Redis host |
| `spring.rabbitmq.host` | RabbitMQ host |

Do not publish real credentials, private keys, access tokens or environment-specific internal addresses.

## 6. Build and Run

Build the application:

```bash
./mvnw clean package
```

If the Maven wrapper is unavailable, use:

```bash
mvn clean package
```

Run the application:

```bash
java -jar target/file-storage-service-0.0.1-SNAPSHOT.jar
```

The default configured application port is `8081`.

## 7. API Endpoints

The service exposes the following main endpoints:

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| GET | `/` | Service status | Public |
| GET | `/health` | Application health response | Required |
| GET | `/files` | List stored files | Bearer token |
| POST | `/files/upload` | Upload a file | Bearer token |
| GET | `/files/download/{filename}` | Download a file | Bearer token |
| GET | `/files/{filename}` | View a file inline | Bearer token |
| DELETE | `/files/{filename}` | Delete a file | Bearer token |
| POST | `/public/api/v1/auth-token` | Request a Keycloak access token | Public |

The `/actuator/health` endpoint is also exposed and permitted without authentication by the current security configuration.

For request examples, response formats, status codes and usage instructions, see:

- [API Documentation](docs/API-DOCUMENTATION.md)
- [User Guide](docs/USER-GUIDE.md)

## 8. Authentication

The service integrates with Keycloak.

To obtain an access token, send a POST request to:

```text
/public/api/v1/auth-token
```

with a JSON request containing `username` and `password`.

Use the returned `accessToken` in requests to protected endpoints:

```text
Authorization: Bearer YOUR_ACCESS_TOKEN
```

Never share access tokens or user passwords.

## 9. File Storage Behaviour

- Files are stored under the configured `filestorage.root` directory.
- The default storage directory is `uploads`.
- Files are organised into date-based directories.
- Stored filenames include a timestamp to make them unique.
- Supported image uploads are processed before being saved.
- File metadata is persisted in PostgreSQL.

The `originalName` field returned by the current API represents the generated stored filename. Use that value when viewing, downloading or deleting the file.

## 10. Keycloak Retry and Circuit Breaker

The service uses Spring Retry to handle temporary Keycloak communication failures.

The authentication flow is separated into two services:

1. `KeycloakAuth` builds the form-encoded authentication request and processes the returned token.
2. `KeycloakTokenClient` sends the HTTP request and applies the retry policy.
3. `KeycloakTokenService` wraps the token request with a Resilience4j circuit breaker.

The retry policy handles:

- `ResourceAccessException`: network and connection-related failures.
- `HttpServerErrorException`: HTTP 5xx responses from Keycloak.

Current retry configuration:

```properties
keycloak.retry.max-attempts=3
keycloak.retry.initial-delay=1000
keycloak.retry.multiplier=2
keycloak.retry.max-delay=4000
```

This allows three total attempts: the initial request followed by retries after approximately one second and two seconds.

The configured Keycloak HTTP connection and read timeouts are 5 seconds each.

HTTP 4xx responses are not included in the retry policy. After eligible failures exhaust their retries, the recovery methods raise an authentication failure.

The circuit breaker can prevent further calls when Keycloak communication is failing.

Passwords and access tokens must never be written to application logs.

## 11. Running Tests

Run the complete test suite:

```bash
./mvnw clean verify
```

Run focused Keycloak retry tests:

```bash
./mvnw -Dtest=KeycloakTokenClientTest,KeycloakRetryBackoffTest,KeycloakRealConnectionFailureTest,KeycloakAuthIntegrationTest test
```

Tests cover file storage, controllers, security, authentication, retry behaviour and supporting services.

## 12. Additional Documentation

| Document | Description |
|---|---|
| [Installation Guide](docs/INSTALLATION-GUIDE.md) | Environment requirements, configuration and startup |
| [User Guide](docs/USER-GUIDE.md) | How to use the API |
| [API Documentation](docs/API-DOCUMENTATION.md) | Endpoint details and request/response examples |
| [Architecture](docs/ARCHITECTURE.md) | Application components and integrations |
