package com.abcbank.filestorage.events;

import java.time.LocalDateTime;

public record FileEvent(
    String eventId,
    String eventType,
    Long fileId,
    String originalName,
    long size,
    LocalDateTime timestamp
) {}

