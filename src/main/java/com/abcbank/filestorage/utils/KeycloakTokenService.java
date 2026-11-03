package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AuthTokenResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;

import java.util.function.Supplier;

@Slf4j
@Service
public class KeycloakTokenService {

    private final KeycloakTokenClient keycloakTokenClient;
    private final CircuitBreaker circuitBreaker;

    public KeycloakTokenService(
            KeycloakTokenClient keycloakTokenClient,
            CircuitBreaker keycloakCircuitBreaker) {
        this.keycloakTokenClient = keycloakTokenClient;
        this.circuitBreaker = keycloakCircuitBreaker;
    }

    public AuthTokenResponse requestToken(
            String endpoint,
            HttpEntity<MultiValueMap<String, String>> request) {

        Supplier<AuthTokenResponse> decoratedSupplier =
                CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        () -> keycloakTokenClient.requestToken(endpoint, request)
                );

        try {
            return decoratedSupplier.get();
        } catch (CallNotPermittedException exception) {
            log.error("Keycloak circuit breaker is OPEN. Request blocked.");
            throw exception;
        }
    }
}
