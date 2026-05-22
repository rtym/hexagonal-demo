package com.example.hexdemo.application.port.in;

import com.example.hexdemo.domain.model.ProcessedMessage;

import java.util.List;

/**
 * Primary (driving) port — lets the REST adapter retrieve stored results
 * without coupling to any specific persistence technology.
 */
public interface GetProcessedMessagesQuery {
    List<ProcessedMessage> getAll();
}
