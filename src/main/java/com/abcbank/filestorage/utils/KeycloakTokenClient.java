package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AuthTokenResponse;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class KeycloakTokenClient {

    private final RestTemplate restTemplate;

    @Value("${keycloak.retry.max-attempts:3}")
    private int maxAttempts;

    public KeycloakTokenClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
        retryFor = {
            ResourceAccessException.class,
            HttpServerErrorException.class
        },
        maxAttemptsExpression = "${keycloak.retry.max-attempts:3}",
        backoff = @Backoff(
            delayExpression = "${keycloak.retry.initial-delay:1000}",
            multiplierExpression = "${keycloak.retry.multiplier:2}",
            maxDelayExpression = "${keycloak.retry.max-delay:4000}"
        )
    )
    public AuthTokenResponse requestToken(
            String endpoint,
            HttpEntity<MultiValueMap<String, String>> request) {

        if (maxAttempts < 1) {
            throw new IllegalStateException(
                "Keycloak retry max-attempts must be at least 1"
            );
        }

        log.info(
            "Sending request to Keycloak. Retry count: {}",
            RetrySynchronizationManager.getContext().getRetryCount()
        );
        return restTemplate.postForObject(
            endpoint,
            request,
            AuthTokenResponse.class
        );
    }

    @Recover
    public AuthTokenResponse recover(
            ResourceAccessException exception,
            String endpoint,
            HttpEntity<MultiValueMap<String, String>> request) {

        log.error(
            "Keycloak network request failed after {} attempts",
            maxAttempts,
            exception
        );

        throw new AuthenticationFailedException(
            "Unable to complete authentication with Keycloak after "
                + maxAttempts + " attempts",
            exception
        );
    }

    @Recover
    public AuthTokenResponse recover(
            HttpServerErrorException exception,
            String endpoint,
            HttpEntity<MultiValueMap<String, String>> request) {

        log.error(
            "Keycloak server request failed after {} attempts",
            maxAttempts,
            exception
        );

        throw new AuthenticationFailedException(
            "Unable to complete authentication with Keycloak after "
                + maxAttempts + " attempts",
            exception
        );
    }

    @Recover
    public AuthTokenResponse recover(
            RuntimeException exception,
            String endpoint,
            HttpEntity<MultiValueMap<String, String>> request) {

        // Preserve exceptions that are not eligible for retry.
        throw exception;
    }
}
