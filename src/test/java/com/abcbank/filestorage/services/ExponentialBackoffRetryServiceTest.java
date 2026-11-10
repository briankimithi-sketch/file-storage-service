package com.abcbank.filestorage.services;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExponentialBackoffRetryServiceTest {

    @Test
    void whenHostOffline_thenRetriesWithExponentialBackoff() {
        ExponentialBackoffRetryService service =
                new ExponentialBackoffRetryService(new RestTemplate());

        service.setMaxRetries(4);
        service.setInitialDelay(500);

        String offlineUrl = "http://localhost:9999/api/data";
        long startTime = System.currentTimeMillis();

        assertThrows(RuntimeException.class, () ->
                service.makeRequestWithExponentialBackoff(offlineUrl)
        );

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration >= 3500);
    }
}
