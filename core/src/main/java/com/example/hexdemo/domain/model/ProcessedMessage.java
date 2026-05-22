package com.example.hexdemo.domain.model;

import java.time.Instant;

/**
 * Result of processing a {@link Message}: the original message type paired with
 * the ASCII-art image produced by whichever plugin handled it.
 */
public record ProcessedMessage(
        String id,
        String messageType,
        String asciiArt,
        MessageSource source,
        Instant processedAt
) {
    public enum MessageSource { FILE, SQS, REST }
}
