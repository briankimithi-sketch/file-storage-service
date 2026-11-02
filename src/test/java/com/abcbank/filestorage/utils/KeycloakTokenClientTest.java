package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AuthTokenResponse;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = KeycloakTokenClientTest.TestConfig.class)
@TestPropertySource(properties = {
        "keycloak.retry.max-attempts=3",
        "keycloak.retry.initial-delay=0",
        "keycloak.retry.multiplier=1",
        "keycloak.retry.max-delay=0"
})
class KeycloakTokenClientTest {

    private static final String ENDPOINT =
            "http://localhost:8080/token";

    private static final String TOKEN =
            "test-access-token";

    @jakarta.annotation.Resource
    private KeycloakTokenClient tokenClient;

    @jakarta.annotation.Resource
    private RestTemplate restTemplate;

    private HttpEntity<MultiValueMap<String, String>> request;

    @BeforeEach
    void setUp() {
        reset(restTemplate);
        request = new HttpEntity<>(new LinkedMultiValueMap<String, String>());
    }

    @Test
    void shouldReturnTokenOnSuccessfulRequest() {
        when(restTemplate.postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        )).thenReturn(new AuthTokenResponse(TOKEN));

        AuthTokenResponse result =
                tokenClient.requestToken(ENDPOINT, request);

        assertNotNull(result);
        assertEquals(TOKEN, result.getAccessToken());

        verify(restTemplate, times(1)).postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        );
    }

    @Test
    void shouldRetryAfterServerErrorAndEventuallySucceed() {
        when(restTemplate.postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        ))
        .thenThrow(HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Server Error",
                null,
                null,
                null
        ))
        .thenReturn(new AuthTokenResponse(TOKEN));

        AuthTokenResponse result =
                tokenClient.requestToken(ENDPOINT, request);

        assertEquals(TOKEN, result.getAccessToken());

        verify(restTemplate, times(2)).postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        );
    }

    @Test
    void shouldRetryAfterNetworkFailureAndEventuallySucceed() {
        when(restTemplate.postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        ))
        .thenThrow(new ResourceAccessException("Connection refused"))
        .thenReturn(new AuthTokenResponse(TOKEN));

        AuthTokenResponse result =
                tokenClient.requestToken(ENDPOINT, request);

        assertEquals(TOKEN, result.getAccessToken());

        verify(restTemplate, times(2)).postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        );
    }

    @Test
    void shouldRecoverAfterMaximumRetryAttempts() {
        when(restTemplate.postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        )).thenThrow(
                new ResourceAccessException("Connection timeout")
        );

        assertThrows(
                AuthenticationFailedException.class,
                () -> tokenClient.requestToken(ENDPOINT, request)
        );

        verify(restTemplate, times(3)).postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        );
    }

    @Test
    void shouldNotRetryClientErrors() {
        when(restTemplate.postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        )).thenThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                null,
                null
        ));

        assertThrows(
                HttpClientErrorException.class,
                () -> tokenClient.requestToken(ENDPOINT, request)
        );

        verify(restTemplate, times(1)).postForObject(
                eq(ENDPOINT),
                any(HttpEntity.class),
                eq(AuthTokenResponse.class)
        );
    }

    @Configuration
    @EnableRetry
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
