package com.example.hexdemo.adapter.in.file;

import com.example.hexdemo.application.port.in.ProcessMessageUseCase;
import com.example.hexdemo.domain.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Driving adapter — polls an input directory for .txt files.
 * Each non-blank line is treated as one message and forwarded to the use case.
 * Processed files are moved to input/processed/ to avoid duplicate handling.
 */
@Slf4j
@Component
public class FileMessageAdapter {

    private final ProcessMessageUseCase processMessageUseCase;
    private final Path inputDir;
    private final Path processedDir;

    public FileMessageAdapter(ProcessMessageUseCase processMessageUseCase,
                              @Value("${adapter.file.input-dir:./input}") String inputDir) {
        this.processMessageUseCase = processMessageUseCase;
        this.inputDir = Paths.get(inputDir);
        this.processedDir = this.inputDir.resolve("processed");
    }

    @Scheduled(fixedDelayString = "${adapter.file.polling-interval-ms:10000}")
    public void poll() {
        if (!Files.exists(inputDir)) return;

        ensureProcessedDir();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir, "*.txt")) {
            for (Path file : stream) {
                processFile(file);
            }
        } catch (IOException e) {
            log.error("Error scanning input directory {}", inputDir, e);
        }
    }

    private void processFile(Path file) {
        log.info("File adapter: processing {}", file.getFileName());
        try {
            Files.lines(file)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(line -> processMessageUseCase.processMessage(
                            new Message(UUID.randomUUID().toString(), line,
                                    Message.MessageSource.FILE, Instant.now())));

            Files.move(file, processedDir.resolve(file.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to process file {}", file, e);
        }
    }

    private void ensureProcessedDir() {
        try {
            Files.createDirectories(processedDir);
        } catch (IOException e) {
            log.warn("Could not create processed dir: {}", e.getMessage());
        }
    }
}
