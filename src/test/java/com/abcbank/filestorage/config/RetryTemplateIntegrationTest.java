package com.abcbank.filestorage.config;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.retry.support.RetryTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RetryTemplateConfig.class,
        RetryListenerConfig.class
})
class RetryTemplateIntegrationTest {

    @Autowired
    private RetryTemplate retryTemplate;

    @Test
    void shouldRetryAndSucceedOnThirdAttempt() {
        AtomicInteger attempts = new AtomicInteger();

        String result = retryTemplate.execute(context -> {
            int attempt = attempts.incrementAndGet();

            if (attempt < 3) {
                throw new IllegalStateException("Temporary failure");
            }

            return "success";
        });

        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void shouldFailAfterThreeAttempts() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(IllegalStateException.class, () ->
                retryTemplate.execute(context -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("Permanent failure");
                })
        );

        assertEquals(3, attempts.get());
    }
}
