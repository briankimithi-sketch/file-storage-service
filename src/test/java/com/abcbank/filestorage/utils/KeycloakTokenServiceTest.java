package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeycloakTokenServiceTest {

    private KeycloakTokenClient tokenClient;
    private CircuitBreaker circuitBreaker;
    private KeycloakTokenService tokenService;

    private static final String ENDPOINT = "http://localhost:8080/token";

    private HttpEntity<MultiValueMap<String, String>> request;

    @BeforeEach
    void setUp() {
        tokenClient = mock(KeycloakTokenClient.class);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .ignoreException(exception ->
                        exception instanceof HttpClientErrorException)
                .recordException(exception ->
                        exception instanceof AuthenticationFailedException)
                .build();

        circuitBreaker = CircuitBreaker.of("keycloak-test", config);

        tokenService = new KeycloakTokenService(
                tokenClient,
                circuitBreaker
        );

        request = new HttpEntity<>(new LinkedMultiValueMap<>());
    }

    @Test
    void shouldOpenCircuitAfterRepeatedInfrastructureFailures() {

        when(tokenClient.requestToken(ENDPOINT, request))
                .thenThrow(new AuthenticationFailedException("Keycloak unavailable"));

        assertThrows(
                AuthenticationFailedException.class,
                () -> tokenService.requestToken(ENDPOINT, request)
        );

        assertThrows(
                AuthenticationFailedException.class,
                () -> tokenService.requestToken(ENDPOINT, request)
        );

        assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreaker.getState()
        );

        assertThrows(
                io.github.resilience4j.circuitbreaker.CallNotPermittedException.class,
                () -> tokenService.requestToken(ENDPOINT, request)
        );

        verify(tokenClient, times(2))
                .requestToken(ENDPOINT, request);
    }

    @Test
    void shouldNotOpenCircuitForInvalidCredentials() {

        when(tokenClient.requestToken(ENDPOINT, request))
                .thenThrow(new HttpClientErrorException(
                        HttpStatus.UNAUTHORIZED
                ));

        assertThrows(
                HttpClientErrorException.class,
                () -> tokenService.requestToken(ENDPOINT, request)
        );

        assertEquals(
                CircuitBreaker.State.CLOSED,
                circuitBreaker.getState()
        );

        assertEquals(
                0,
                circuitBreaker.getMetrics().getNumberOfBufferedCalls(),
                "401 responses should be ignored by circuit-breaker metrics"
        );

        verify(tokenClient, times(1))
                .requestToken(ENDPOINT, request);
    }
}
