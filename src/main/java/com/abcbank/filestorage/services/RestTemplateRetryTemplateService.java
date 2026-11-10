package com.abcbank.filestorage.services;

import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class RestTemplateRetryTemplateService {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public RestTemplateRetryTemplateService(
            RestTemplate restTemplate,
            RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
    }

    public String fetchData(String url) {
        RetryCallback<String, ResourceAccessException> retryCallback =
                context -> restTemplate.getForObject(url, String.class);

        RecoveryCallback<String> recoveryCallback =
                context -> "Fallback response after all retries failed for: " + url;

        return retryTemplate.execute(retryCallback, recoveryCallback);
    }
}
