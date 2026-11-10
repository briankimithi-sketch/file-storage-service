package com.abcbank.filestorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryListenerConfig {

    @Bean
    public DefaultListenerSupport defaultListenerSupport() {
        return new DefaultListenerSupport();
    }
}
