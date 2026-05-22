package com.example.hexdemo.application.service;

import com.example.hexdemo.application.port.out.ImageGeneratorPort;
import com.example.hexdemo.application.port.out.MessageStoragePort;
import com.example.hexdemo.domain.model.Message;
import com.example.hexdemo.domain.model.ProcessedMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessingServiceTest {

    @Mock
    private ImageGeneratorPort imageGeneratorPort;

    @Mock
    private MessageStoragePort storagePort;

    @InjectMocks
    private MessageProcessingService service;

    @Test
    void processMessage_with_known_type_stores_generated_art() {
        when(imageGeneratorPort.generateImage("carrot")).thenReturn(Optional.of("ascii-carrot"));

        service.processMessage(message("carrot"));

        ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(storagePort).save(captor.capture());
        assertThat(captor.getValue().asciiArt()).isEqualTo("ascii-carrot");
        assertThat(captor.getValue().messageType()).isEqualTo("carrot");
    }

    @Test
    void processMessage_with_unknown_type_stores_placeholder() {
        when(imageGeneratorPort.generateImage("banana")).thenReturn(Optional.empty());

        service.processMessage(message("banana"));

        ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(storagePort).save(captor.capture());
        assertThat(captor.getValue().asciiArt()).contains("No plugin for: banana");
    }

    @Test
    void processMessage_normalises_type_to_lowercase() {
        when(imageGeneratorPort.generateImage("carrot")).thenReturn(Optional.of("art"));

        service.processMessage(message("  CARROT  "));

        verify(imageGeneratorPort).generateImage("carrot");
    }

    @Test
    void processMessage_preserves_original_message_id() {
        when(imageGeneratorPort.generateImage(any())).thenReturn(Optional.of("art"));

        service.processMessage(message("id-42", "rabbit"));

        ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(storagePort).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("id-42");
    }

    @Test
    void processMessage_sets_processedAt_to_a_recent_timestamp() {
        when(imageGeneratorPort.generateImage(any())).thenReturn(Optional.of("art"));
        Instant before = Instant.now();

        service.processMessage(message("carrot"));

        ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(storagePort).save(captor.capture());
        assertThat(captor.getValue().processedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void getAll_delegates_to_storage_port() {
        ProcessedMessage pm = processedMessage("id-1", "carrot");
        when(storagePort.findAll()).thenReturn(List.of(pm));

        List<ProcessedMessage> result = service.getAll();

        assertThat(result).containsExactly(pm);
    }

    @Test
    void getAll_returns_empty_list_when_no_messages_stored() {
        when(storagePort.findAll()).thenReturn(List.of());

        assertThat(service.getAll()).isEmpty();
    }

    @Test
    void processMessage_propagates_file_source_to_processed_message() {
        when(imageGeneratorPort.generateImage("carrot")).thenReturn(Optional.of("art"));

        service.processMessage(new Message("id-1", "carrot", Message.MessageSource.FILE, Instant.now()));

        ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(storagePort).save(captor.capture());
        assertThat(captor.getValue().source()).isEqualTo(ProcessedMessage.MessageSource.FILE);
    }

    @Test
    void processMessage_whitespace_only_content_stores_placeholder_for_empty_type() {
        when(imageGeneratorPort.generateImage("")).thenReturn(Optional.empty());

        service.processMessage(message("   "));

        ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(storagePort).save(captor.capture());
        assertThat(captor.getValue().messageType()).isEmpty();
        assertThat(captor.getValue().asciiArt()).contains("No plugin for:");
    }

    @Test
    void processMessage_placeholder_includes_plugin_retry_hint() {
        when(imageGeneratorPort.generateImage("unknown")).thenReturn(Optional.empty());

        service.processMessage(message("unknown"));

        ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(storagePort).save(captor.capture());
        assertThat(captor.getValue().asciiArt()).contains("plugins/");
    }

    @Test
    void processMessage_calls_image_generator_exactly_once() {
        when(imageGeneratorPort.generateImage("rabbit")).thenReturn(Optional.of("art"));

        service.processMessage(message("rabbit"));

        verify(imageGeneratorPort, times(1)).generateImage("rabbit");
        verifyNoMoreInteractions(imageGeneratorPort);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static Message message(String content) {
        return message("test-id", content);
    }

    private static Message message(String id, String content) {
        return new Message(id, content, Message.MessageSource.REST, Instant.now());
    }

    private static ProcessedMessage processedMessage(String id, String type) {
        return new ProcessedMessage(id, type, "art", ProcessedMessage.MessageSource.REST, Instant.now());
    }
}
