package com.example.hexdemo.application.port.out;

import com.example.hexdemo.domain.model.ProcessedMessage;

import java.util.List;

/**
 * Secondary (driven) port — the application core writes and reads processed
 * messages through this interface; the DynamoDB adapter implements it.
 */
public interface MessageStoragePort {
    void save(ProcessedMessage processedMessage);
    List<ProcessedMessage> findAll();
}
