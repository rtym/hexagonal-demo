package com.example.hexdemo.application.port.in;

import com.example.hexdemo.domain.model.Message;

/**
 * Primary (driving) port — called by every inbound adapter to hand a raw message
 * to the application core.
 */
public interface ProcessMessageUseCase {
    void processMessage(Message message);
}
