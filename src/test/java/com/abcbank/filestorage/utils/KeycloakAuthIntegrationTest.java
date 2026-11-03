package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AccessTokenDto;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.annotation.Resource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = KeycloakAuthIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "keycloak.retry.max-attempts=3",
        "keycloak.retry.initial-delay=20",
        "keycloak.retry.multiplier=2",
        "keycloak.retry.max-delay=100"
})
class KeycloakAuthIntegrationTest {

    private HttpServer server;

    private final AtomicInteger requestCount = new AtomicInteger(0);

    private final AtomicReference<String> receivedBody =
            new AtomicReference<>("");

    private final AtomicReference<String> receivedContentType =
            new AtomicReference<>("");

    @Resource
    private KeycloakTokenClient tokenClient;

    private KeycloakAuth keycloakAuth;

    @BeforeEach
    void setUp() throws IOException {

        requestCount.set(0);
        receivedBody.set("");
        receivedContentType.set("");

        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext("/token", exchange -> {

            int attempt = requestCount.incrementAndGet();

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            receivedBody.set(body);

            receivedContentType.set(
                    exchange.getRequestHeaders().getFirst("Content-Type")
            );

            String response;
            int status;

            if (attempt == 1) {
                status = HttpStatus.SERVICE_UNAVAILABLE.value();
                response = "{\"error\":\"temporarily_unavailable\"}";
            } else {
                status = HttpStatus.OK.value();
                response = "{\"access_token\":\"integration-test-token\"}";
            }

            byte[] responseBytes =
                    response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json"
            );

            exchange.sendResponseHeaders(status, responseBytes.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(responseBytes);
            }
        });

        server.start();

        String endpoint = "http://127.0.0.1:"
                + server.getAddress().getPort() + "/token";

        keycloakAuth = new KeycloakAuth(tokenClient);

        ReflectionTestUtils.setField(
                keycloakAuth,
                "keyCloakEndpoint",
                endpoint
        );

        ReflectionTestUtils.setField(
                keycloakAuth,
                "keyCloakClientId",
                "file-storage"
        );
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldRetryAndAuthenticateSuccessfullyAgainstLocalServer() {

        AccessTokenDto result = keycloakAuth.getAuthToken(
                "test-user",
                "test-password"
        );

        assertNotNull(result);

        assertEquals(
                "integration-test-token",
                result.getAccessToken()
        );

        assertEquals(
                2,
                requestCount.get(),
                "Expected one failed request followed by one successful retry"
        );

        assertTrue(
                receivedContentType.get().startsWith(
                        "application/x-www-form-urlencoded"
                ),
                "Expected form URL encoded content type"
        );

        assertTrue(receivedBody.get().contains("client_id=file-storage"));
        assertTrue(receivedBody.get().contains("username=test-user"));
        assertTrue(receivedBody.get().contains("password=test-password"));
        assertTrue(receivedBody.get().contains("grant_type=password"));
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        KeycloakTokenClient keycloakTokenClient(
                RestTemplate restTemplate
        ) {
            return new KeycloakTokenClient(restTemplate);
        }
    }
}
