package com.abcbank.filestorage.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

@Slf4j
public class DefaultListenerSupport extends RetryListenerSupport {

    @Override
    public <T, E extends Throwable> boolean open(
            RetryContext context,
            RetryCallback<T, E> callback) {

        log.info("Retry operation started");

        return super.open(context, callback);
    }

    @Override
    public <T, E extends Throwable> void onError(
            RetryContext context,
            RetryCallback<T, E> callback,
            Throwable throwable) {

        log.warn(
                "Retry attempt {} failed: {}",
                context.getRetryCount(),
                throwable.getMessage()
        );

        super.onError(context, callback, throwable);
    }

    @Override
    public <T, E extends Throwable> void close(
            RetryContext context,
            RetryCallback<T, E> callback,
            Throwable throwable) {

        if (throwable == null) {
            log.info("Retry operation completed successfully");
        } else {
            log.error(
                    "Retry operation exhausted after {} attempts",
                    context.getRetryCount(),
                    throwable
            );
        }

        super.close(context, callback, throwable);
    }
}
