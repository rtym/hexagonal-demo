package com.example.hexdemo.domain.model;

import java.time.Instant;

/**
 * Inbound message received from any driving adapter (file, SQS, REST).
 * Pure domain object — no framework annotations, no persistence mapping.
 */
public record Message(
        String id,
        String content,
        MessageSource source,
        Instant receivedAt
) {
    public enum MessageSource { FILE, SQS, REST }
}
