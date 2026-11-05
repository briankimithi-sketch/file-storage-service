package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AuthTokenResponse;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
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

  @Value("${keycloak.retry.initial-delay:1000}")
  private long initialDelay;

  @Value("${keycloak.retry.multiplier:2}")
  private double multiplier;

  @Value("${keycloak.retry.max-delay:4000}")
  private long maxDelay;

  public KeycloakTokenClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public AuthTokenResponse requestToken(
      String endpoint, HttpEntity<MultiValueMap<String, String>> request) {

    if (maxAttempts < 1) {
      throw new IllegalStateException("Keycloak retry max-attempts must be at least 1");
    }

    int attempt = 1;
    long delay = initialDelay;

    while (attempt <= maxAttempts) {
      try {
        log.info("Sending request to Keycloak (attempt {}/{})", attempt, maxAttempts);

        return restTemplate.postForObject(endpoint, request, AuthTokenResponse.class);

      } catch (ResourceAccessException | HttpServerErrorException exception) {

        if (attempt == maxAttempts) {
          log.error("Keycloak request failed after {} attempts", attempt, exception);

          throw new AuthenticationFailedException(
              "Unable to complete authentication with Keycloak after " + attempt + " attempts",
              exception);
        }

        log.warn(
            "Keycloak request failed on attempt {}. Retrying in {} ms. Cause: {}",
            attempt,
            delay,
            exception.getMessage());

        try {
          Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();

          throw new AuthenticationFailedException(
              "Interrupted while waiting to retry Keycloak request", interruptedException);
        }

        attempt++;
        delay = Math.min(maxDelay, Math.round(delay * multiplier));
      }
    }

    throw new IllegalStateException("Keycloak retry loop terminated unexpectedly");
  }
}
