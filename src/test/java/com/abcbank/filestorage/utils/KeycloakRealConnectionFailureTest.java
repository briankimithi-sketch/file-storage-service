package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AuthTokenResponse;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = KeycloakRealConnectionFailureTest.TestConfig.class
)
@TestPropertySource(properties = {
        "keycloak.retry.max-attempts=3",
        "keycloak.retry.initial-delay=0",
        "keycloak.retry.multiplier=1",
        "keycloak.retry.max-delay=0"
})
class KeycloakRealConnectionFailureTest {

    @jakarta.annotation.Resource
    private KeycloakTokenClient tokenClient;

    @jakarta.annotation.Resource
    private CountingRestTemplate restTemplate;

    @Test
    void shouldRetryRealConnectionFailureAndRecover() throws Exception {

        int unavailablePort;

        // Find an available local port, then close it.
        // Nothing should be listening on that port.
        try (ServerSocket socket = new ServerSocket(0)) {
            unavailablePort = socket.getLocalPort();
        }

        String endpoint =
                "http://127.0.0.1:" + unavailablePort + "/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("client_id", "test-client");
        form.add("username", "test-user");
        form.add("password", "test-password");
        form.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(form);

        AuthenticationFailedException exception = assertThrows(
                AuthenticationFailedException.class,
                () -> tokenClient.requestToken(endpoint, request)
        );

        assertInstanceOf(
                ResourceAccessException.class,
                exception.getCause()
        );

        // Three total attempts: one original request and two retries.
        assertEquals(3, restTemplate.getAttempts());
    }

    @Configuration
    static class TestConfig {

        @Bean
        CountingRestTemplate restTemplate() {
            return new CountingRestTemplate();
        }

        @Bean
        KeycloakTokenClient keycloakTokenClient(
                CountingRestTemplate restTemplate
        ) {
            return new KeycloakTokenClient(restTemplate);
        }
    }

    static class CountingRestTemplate extends RestTemplate {

        private final AtomicInteger attempts = new AtomicInteger();

        @Override
        public <T> T postForObject(
                String url,
                Object request,
                Class<T> responseType,
                Object... uriVariables
        ) {
            attempts.incrementAndGet();

            return super.postForObject(
                    url,
                    request,
                    responseType,
                    uriVariables
            );
        }

        int getAttempts() {
            return attempts.get();
        }
    }
}
