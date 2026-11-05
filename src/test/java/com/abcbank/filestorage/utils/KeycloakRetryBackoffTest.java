
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = KeycloakRetryBackoffTest.TestConfig.class)
@TestPropertySource(properties = {
        "keycloak.retry.max-attempts=3",
        "keycloak.retry.initial-delay=200",
        "keycloak.retry.multiplier=2",
        "keycloak.retry.max-delay=1000"
})
class KeycloakRetryBackoffTest {

    private static final String ENDPOINT =
            "http://localhost:8080/token";

    @jakarta.annotation.Resource
    private KeycloakTokenClient tokenClient;

    @jakarta.annotation.Resource
    private RestTemplate restTemplate;

    @Test
    void shouldApplyExponentialBackoffBetweenRetries() {

        when(restTemplate.postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        )).thenThrow(
                new ResourceAccessException("Connection refused")
        );

        long startTime = System.nanoTime();

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(
                        new org.springframework.util.LinkedMultiValueMap<String, String>()
                );

        assertThrows(
                AuthenticationFailedException.class,
                () -> tokenClient.requestToken(ENDPOINT, request)
        );

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startTime
        );

        verify(restTemplate, times(3)).postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        );

        assertTrue(
                elapsedMillis >= 500,
                "Expected at least 500ms of retry waiting, but got "
                        + elapsedMillis + "ms"
        );
    }

    @Configuration
    static class TestConfig {

        @Bean
        RestTemplate restTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        KeycloakTokenClient keycloakTokenClient(
                RestTemplate restTemplate
        ) {
            return new KeycloakTokenClient(restTemplate);
        }
    }
}
