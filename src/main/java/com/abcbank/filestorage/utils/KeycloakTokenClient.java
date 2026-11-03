package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AuthTokenResponse;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class KeycloakTokenClient {

    private final RestTemplate restTemplate;

    public KeycloakTokenClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
        retryFor = {
            ResourceAccessException.class,
            HttpServerErrorException.class
        },
        maxAttemptsExpression = "#{${keycloak.retry.max-attempts:3}}",
        backoff = @Backoff(
            delayExpression = "#{${keycloak.retry.initial-delay:1000}}",
            multiplierExpression = "#{${keycloak.retry.multiplier:2}}",
            maxDelayExpression = "#{${keycloak.retry.max-delay:4000}}"
        )
    )
    public AuthTokenResponse requestToken(
            String endpoint,
            HttpEntity<MultiValueMap<String, String>> request) {

        int attempt = RetrySynchronizationManager.getContext().getRetryCount() + 1;
        log.info("Sending request to Keycloak (attempt {})", attempt);

        return restTemplate.postForObject(
            endpoint,
            request,
            AuthTokenResponse.class
        );
    }

    @Recover
    public AuthTokenResponse recover(
            HttpClientErrorException exception,
            String endpoint,
            HttpEntity<MultiValueMap<String, String>> request) {

        log.error("Keycloak rejected authentication request: {}",
                exception.getMessage());

        throw exception;
    }

    @Recover
    public AuthTokenResponse recover(
            Exception exception,
            String endpoint,
            HttpEntity<MultiValueMap<String, String>> request) {

        log.error("Keycloak request failed: {}",
                exception.getMessage());

        throw new AuthenticationFailedException(
            "Unable to complete authentication with Keycloak",
            exception
        );
    }
}
