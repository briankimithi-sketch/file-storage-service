# File Storage Service – Installation and Deployment Guide

## 1. Introduction

This guide explains how to prepare the environment, configure, build and run the File Storage Service.

The application is built with Java 17 and Spring Boot 3.2.0.

## 2. Software Requirements

Install the following software:

| Software | Purpose |
|---|---|
| Java JDK 17 | Runs the Spring Boot application |
| Maven 3.8+ | Builds and manages the project |
| PostgreSQL | Stores file metadata |
| Keycloak | Handles user authentication |
| Redis | Supports caching integration |
| RabbitMQ | Supports messaging integration |
| Git | Source code management |

Redis and RabbitMQ should be available when their corresponding application integrations are enabled.

## 3. Verify Java and Maven

Check the Java installation:

    java -version

The installed version should be Java 17.

Check Maven:

    mvn -version

If the project includes the Maven wrapper, it can be used instead:

    ./mvnw -version

## 4. Obtain the Source Code

Clone the project repository:

    git clone <repository-url>

Enter the project directory:

    cd file-storage-service

Use the repository URL supplied by the project administrator.

## 5. Configure the Application

Main configuration file:

    src/main/resources/application.properties

Local development configuration:

    src/main/resources/application-local.properties

The local configuration file may be excluded from Git. Do not commit local credentials or environment-specific settings.

### Database configuration

The application uses PostgreSQL.

Configure the following environment variables:

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_HOST` | PostgreSQL server hostname |
| `SPRING_DATASOURCE_PORT` | PostgreSQL server port |
| `SPRING_DATASOURCE_DB` | Database name |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

The configured database must exist and be accessible to the application.

### Keycloak configuration

Configure the Keycloak integration for the target environment.

Required settings include:

- Keycloak server URL.
- Realm and token endpoint.
- Client identifier.
- Valid user credentials.
- JWT issuer URI used by Spring Security.

The token endpoint and JWT issuer must refer to the correct Keycloak environment.

Do not place production passwords or client secrets in source control.

### File storage configuration

The following properties control file storage:

| Property | Purpose |
|---|---|
| `filestorage.root` | Directory where uploaded files are stored |
| `filestorage.base-url` | Base URL used to construct file links |

Ensure the application has permission to create, read and delete files in the configured storage directory.

### Redis and RabbitMQ

Configure the Redis and RabbitMQ connection settings for the environment where these integrations are enabled.

Do not use development credentials in production.

## 6. Build the Application

From the project root, run:

    ./mvnw clean package

If Maven Wrapper is unavailable, use:

    mvn clean package

This command cleans previous build output, compiles the application, runs the configured tests and packages the application.

The generated JAR is expected at:

    target/file-storage-service-0.0.1-SNAPSHOT.jar

## 7. Run the Application

Start the application using Maven:

    ./mvnw spring-boot:run

Alternatively, run the packaged JAR:

    java -jar target/file-storage-service-0.0.1-SNAPSHOT.jar

The default configured application port is 8081.

Open the following address to check the service:

    http://localhost:8081/

Expected response:

    {
      "service": "file-storage-service",
      "status": "running"
    }

## 8. Verify the Application

Check the service information endpoint:

    curl http://localhost:8081/

Check the Actuator health endpoint:

    curl http://localhost:8081/actuator/health

Open Swagger UI:

    http://localhost:8081/swagger-ui/index.html

If the application cannot connect to PostgreSQL, Keycloak, Redis or RabbitMQ, check the relevant service status and configuration.

## 9. Docker Deployment

The project contains a Dockerfile.

Before building a Docker image, verify that the application port and Docker configuration agree.

The current application configuration uses port 8081, while the Dockerfile declares `EXPOSE 8080`.

The Docker `EXPOSE` instruction does not change the port on which Spring Boot listens. Confirm the intended container port and deployment mapping before building or deploying.

After confirming the port configuration, a Docker image can be built with:

    docker build -t file-storage-service .

Run the container with the environment variables and network configuration required by the deployment environment.

Do not place passwords, private keys or production credentials directly in a Dockerfile or image.

## 10. File Storage and Persistence

Uploaded files are stored in the configured storage directory.

The service organizes files into date-based directories.

File metadata is stored in PostgreSQL.

For reliable deployment:

- Use persistent storage for uploaded files.
- Back up the PostgreSQL database.
- Back up the uploaded file directory.
- Ensure the application has the correct filesystem permissions.
- Monitor available disk space.

A database backup alone does not preserve the actual uploaded documents.

## 11. Running Tests

Run the complete test suite:

    ./mvnw test

Run the full Maven verification lifecycle:

    ./mvnw clean verify

Review the Maven output for failed tests before deploying.

## 12. Troubleshooting

| Problem | What to check |
|---|---|
| Application does not start | Java version, application logs and port availability |
| Database connection fails | PostgreSQL status, hostname, port, database and credentials |
| Authentication fails | Keycloak URL, realm, client and user credentials |
| 401 responses | Access token validity and JWT issuer configuration |
| File upload fails | File type, file size and storage directory permissions |
| File download fails | Filename, database record and physical file existence |
| Docker container cannot be reached | Container port, application port and port mapping |
| Redis connection fails | Redis availability and connection settings |
| RabbitMQ connection fails | RabbitMQ availability and connection settings |

## 13. Production Deployment Checklist

Before deployment, confirm:

- [ ] Java 17 runtime is available.
- [ ] Production configuration is supplied securely.
- [ ] PostgreSQL is reachable.
- [ ] Keycloak authentication is configured.
- [ ] Redis and RabbitMQ are configured if required.
- [ ] File storage uses persistent storage.
- [ ] Application and container ports are aligned.
- [ ] Tests pass.
- [ ] Database and file backups are configured.
- [ ] Secrets and private keys are excluded from Git.
- [ ] Health checks and application logging are available.

## 14. Security Considerations

- Never commit passwords, access tokens or private keys.
- Use HTTPS in deployed environments.
- Restrict access to the file storage directory.
- Use appropriate database permissions.
- Protect uploaded documents through authentication and authorization.
- Keep Java dependencies and server components updated.
- Review logs to ensure sensitive information is not exposed.
