package com.example.hexdemo.application.service;

import com.example.hexdemo.application.port.in.GetProcessedMessagesQuery;
import com.example.hexdemo.application.port.in.ProcessMessageUseCase;
import com.example.hexdemo.application.port.out.ImageGeneratorPort;
import com.example.hexdemo.application.port.out.MessageStoragePort;
import com.example.hexdemo.domain.model.Message;
import com.example.hexdemo.domain.model.ProcessedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Central application service — orchestrates the two use cases.
 *
 * Dependencies are injected via ports (interfaces only); this class knows nothing
 * about DynamoDB, AWS SQS, files, or how plugins are loaded.
 */
@Service
public class MessageProcessingService implements ProcessMessageUseCase, GetProcessedMessagesQuery {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessingService.class);

    private final ImageGeneratorPort imageGeneratorPort;
    private final MessageStoragePort storagePort;

    public MessageProcessingService(ImageGeneratorPort imageGeneratorPort,
                                    MessageStoragePort storagePort) {
        this.imageGeneratorPort = imageGeneratorPort;
        this.storagePort = storagePort;
    }

    @Override
    public void processMessage(Message message) {
        String type = message.content().trim().toLowerCase();
        log.info("Processing message id={} type={} source={}", message.id(), type, message.source());

        String art = imageGeneratorPort.generateImage(type)
                .orElseGet(() -> {
                    log.warn("No plugin loaded for type '{}' — storing placeholder", type);
                    return "[ No plugin for: " + type + " ]\n"
                            + "Drop a plugin JAR into the plugins/ directory and retry.";
                });

        ProcessedMessage result = new ProcessedMessage(
                message.id(),
                type,
                art,
                ProcessedMessage.MessageSource.valueOf(message.source().name()),
                Instant.now()
        );
        storagePort.save(result);
        log.info("Stored processed message id={}", result.id());
    }

    @Override
    public List<ProcessedMessage> getAll() {
        return storagePort.findAll();
    }
}
