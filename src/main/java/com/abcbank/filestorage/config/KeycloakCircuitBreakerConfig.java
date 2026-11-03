package com.abcbank.filestorage.config;

import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
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

    @Bean
    public MeterBinder keycloakCircuitBreakerMetrics(
            CircuitBreaker keycloakCircuitBreaker) {

        return registry -> {
            Gauge.builder("keycloak.circuitbreaker.failure.rate",
                    keycloakCircuitBreaker,
                    cb -> cb.getMetrics().getFailureRate())
                    .description("Keycloak circuit-breaker failure rate percentage")
                    .register(registry);

            Gauge.builder("keycloak.circuitbreaker.buffered.calls",
                    keycloakCircuitBreaker,
                    cb -> cb.getMetrics().getNumberOfBufferedCalls())
                    .description("Number of calls in the circuit-breaker sliding window")
                    .register(registry);

            Gauge.builder("keycloak.circuitbreaker.failed.calls",
                    keycloakCircuitBreaker,
                    cb -> cb.getMetrics().getNumberOfFailedCalls())
                    .description("Number of failed calls recorded by the circuit breaker")
                    .register(registry);

            Gauge.builder("keycloak.circuitbreaker.not.permitted.calls",
                    keycloakCircuitBreaker,
                    cb -> cb.getMetrics().getNumberOfNotPermittedCalls())
                    .description("Number of calls rejected while the circuit breaker is open")
                    .register(registry);

            Gauge.builder("keycloak.circuitbreaker.state",
                    keycloakCircuitBreaker,
                    cb -> cb.getState() == CircuitBreaker.State.CLOSED ? 1 : 0)
                    .tag("state", "closed")
                    .description("Whether the Keycloak circuit breaker is closed")
                    .register(registry);

            Gauge.builder("keycloak.circuitbreaker.state",
                    keycloakCircuitBreaker,
                    cb -> cb.getState() == CircuitBreaker.State.OPEN ? 1 : 0)
                    .tag("state", "open")
                    .description("Whether the Keycloak circuit breaker is open")
                    .register(registry);

            Gauge.builder("keycloak.circuitbreaker.state",
                    keycloakCircuitBreaker,
                    cb -> cb.getState() == CircuitBreaker.State.HALF_OPEN ? 1 : 0)
                    .tag("state", "half_open")
                    .description("Whether the Keycloak circuit breaker is half-open")
                    .register(registry);
        };
    }
}
