package com.abcbank.filestorage.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestTemplateRetryServiceTest {

    @Test
    void shouldRetryRequestWhenHostIsOffline() {
        RestTemplateRetryService service = new RestTemplateRetryService();

        long startTime = System.currentTimeMillis();

        assertThrows(
                RuntimeException.class,
                () -> service.makeRequestWithRetry(
                        "http://localhost:9999/api/data"
                )
        );

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(
                duration >= 4000,
                "Expected at least 4 seconds due to retry delays"
        );
    }
}
