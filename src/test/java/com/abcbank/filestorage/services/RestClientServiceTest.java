package com.abcbank.filestorage.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RestClientServiceTest {

    @Autowired
    private RestClientService restClientService;

    @Test
    void whenHostOffline_thenRetriesAndReturnsFallback() {
        String offlineUrl = "http://localhost:9999/api/data";

        String response = restClientService.fetchData(offlineUrl);

        assertTrue(
                response.contains("Fallback response"),
                "Expected the recovery method to return a fallback response"
        );
    }
}
