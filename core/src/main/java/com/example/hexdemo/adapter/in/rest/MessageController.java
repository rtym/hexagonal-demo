package com.example.hexdemo.adapter.in.rest;

import com.example.hexdemo.application.port.in.GetProcessedMessagesQuery;
import com.example.hexdemo.application.port.in.ProcessMessageUseCase;
import com.example.hexdemo.application.port.out.ImageGeneratorPort;
import com.example.hexdemo.domain.model.Message;
import com.example.hexdemo.domain.model.ProcessedMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Driving adapter — exposes three REST endpoints:
 *
 *   GET  /api/messages          → all processed messages (used by the static frontend)
 *   POST /api/messages          → submit a message manually (demo / testing)
 *   GET  /api/plugins           → list currently loaded plugins (observability)
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MessageController {

    private final ProcessMessageUseCase processMessageUseCase;
    private final GetProcessedMessagesQuery getProcessedMessagesQuery;
    private final ImageGeneratorPort imageGeneratorPort;

    public MessageController(ProcessMessageUseCase processMessageUseCase,
                             GetProcessedMessagesQuery getProcessedMessagesQuery,
                             ImageGeneratorPort imageGeneratorPort) {
        this.processMessageUseCase = processMessageUseCase;
        this.getProcessedMessagesQuery = getProcessedMessagesQuery;
        this.imageGeneratorPort = imageGeneratorPort;
    }

    @GetMapping("/messages")
    public List<ProcessedMessage> getMessages() {
        return getProcessedMessagesQuery.getAll();
    }

    @PostMapping("/messages")
    public ResponseEntity<Map<String, String>> submitMessage(@RequestBody String content) {
        String id = UUID.randomUUID().toString();
        processMessageUseCase.processMessage(
                new Message(id, content.trim(), Message.MessageSource.REST, Instant.now()));
        return ResponseEntity.accepted().body(Map.of("id", id, "status", "queued"));
    }

    @GetMapping("/plugins")
    public Map<String, Object> getPlugins() {
        return Map.of(
                "loaded", imageGeneratorPort.getSupportedTypes().size(),
                "supportedTypes", imageGeneratorPort.getSupportedTypes()
        );
    }
}
