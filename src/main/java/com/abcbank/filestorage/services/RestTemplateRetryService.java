package com.abcbank.filestorage.services;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class RestTemplateRetryService {

    private RestTemplate restTemplate = new RestTemplate();
    private int maxRetries = 3;
    private long retryDelay = 2000;

    public String makeRequestWithRetry(String url) {
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return restTemplate.getForObject(url, String.class);
            } catch (ResourceAccessException e) {
                attempt++;

                if (attempt >= maxRetries) {
                    throw new RuntimeException(
                            "Failed after " + maxRetries + " attempts",
                            e
                    );
                }

                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        throw new RuntimeException("Unexpected error in retry logic");
    }
}
