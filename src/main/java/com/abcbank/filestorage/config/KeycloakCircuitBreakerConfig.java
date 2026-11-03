package com.abcbank.filestorage.config;

import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class KeycloakCircuitBreakerConfig {

    @Bean
    public CircuitBreaker keycloakCircuitBreaker(
            @Value("${keycloak.circuit-breaker.sliding-window-size:10}")
            int slidingWindowSize,

            @Value("${keycloak.circuit-breaker.minimum-number-of-calls:5}")
            int minimumNumberOfCalls,

            @Value("${keycloak.circuit-breaker.failure-rate-threshold:50}")
            float failureRateThreshold,

            @Value("${keycloak.circuit-breaker.wait-duration:10000}")
            long waitDuration,

            @Value("${keycloak.circuit-breaker.permitted-calls-in-half-open:3}")
            int permittedCallsInHalfOpen) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofMillis(waitDuration))
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpen)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .ignoreException(exception ->
                        exception instanceof HttpClientErrorException)
                .recordException(exception ->
                        exception instanceof AuthenticationFailedException)
                .build();

        return CircuitBreaker.of("keycloak", config);
    }
}
