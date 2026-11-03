# File Storage Service

Spring Boot file storage service with Keycloak authentication.

## Technology

- Java 17
- Spring Boot 3.2.0
- Maven
- Spring Security
- Spring Retry
- RestTemplate
- JUnit 5

## Keycloak Authentication Retry

The service uses Spring Retry to handle temporary Keycloak communication failures.

### Dependencies

The following dependencies are included in `pom.xml`:

- `org.springframework.retry:spring-retry`
- `org.springframework:spring-aspects`

Spring Boot dependency management supplies compatible dependency versions.

### Enabling retry

Retry support is enabled on the main application class:

```java
@EnableRetry
@SpringBootApplication
public class FileStorageServiceApplication {
}

```

### Implementation structure

The authentication flow is separated into two services:

1. `KeycloakAuth` builds the form-encoded authentication request and processes the returned token.
2. `KeycloakTokenClient` sends the HTTP request and applies the retry policy.

Keeping the retryable method in a separate Spring-managed bean allows Spring's retry proxy to intercept calls correctly.

### Retry policy

The `requestToken` method retries:

- `ResourceAccessException`: network and connection-related failures.
- `HttpServerErrorException`: HTTP 5xx responses from Keycloak.

Current configuration:

```properties
keycloak.retry.max-attempts=3
keycloak.retry.initial-delay=1000
keycloak.retry.multiplier=2
keycloak.retry.max-delay=4000
```

This means three total attempts: the initial request, followed by retries after approximately one second and two seconds.

### HTTP timeouts

```properties
keycloak.http.connect-timeout=5000
keycloak.http.read-timeout=5000
```

Both timeout values are in milliseconds.

### Recovery behavior

After retries are exhausted:

- HTTP 4xx errors are rethrown without retrying.
- Other exhausted retry failures are wrapped in `AuthenticationFailedException`.

Passwords and access tokens must never be written to application logs.

## Tests

| Test | Purpose |
|---|---|
| `KeycloakTokenClientTest` | Success, retry, recovery and HTTP 4xx handling |
| `KeycloakRealConnectionFailureTest` | Actual connection-refused scenario |
| `KeycloakRetryBackoffTest` | Exponential retry delay |
| `KeycloakAuthTest` | Authentication service behavior |
| `KeycloakAuthIntegrationTest` | Complete authentication flow against a local HTTP server |

The integration test uses a local HTTP server. It does not contact the organization's Keycloak UAT server.

## Running tests

Run the focused retry tests:

```bash
mvn -Dtest=KeycloakTokenClientTest,KeycloakRetryBackoffTest,KeycloakRealConnectionFailureTest,KeycloakAuthIntegrationTest test
```

Run the complete test suite:

```bash
mvn clean verify
```

## Configuration and security

Keep environment-specific configuration and credentials outside version control.

Retry settings can be overridden through Spring configuration properties without changing the Java implementation.
