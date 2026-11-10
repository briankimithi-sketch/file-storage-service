package com.abcbank.filestorage.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RestTemplateRetryTemplateServiceTest {

    @Autowired
    private RestTemplateRetryTemplateService service;

    @Test
    void whenHostOffline_thenRetriesAndReturnsFallback() {
        String response = service.fetchData("http://localhost:9999/api/data");

        assertTrue(
                response.contains("Fallback response"),
                "Expected the recovery callback to return a fallback response"
        );
    }
}
