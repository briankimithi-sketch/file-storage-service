package com.abcbank.filestorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryTemplateConfig {

    private FixedBackOffPolicy fixedBackOffPolicy(long backOffPeriod) {
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(backOffPeriod);
        return policy;
    }

    @Bean
    public RetryTemplate retryTemplate(
            DefaultListenerSupport listenerSupport) {

        return RetryTemplate.builder()
                .maxAttempts(3)
                .customBackoff(fixedBackOffPolicy(2000L))
                .withListener(listenerSupport)
                .build();
    }
}
