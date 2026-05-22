package com.example.hexdemo.adapter.in.rest;

import com.example.hexdemo.application.port.in.GetProcessedMessagesQuery;
import com.example.hexdemo.application.port.in.ProcessMessageUseCase;
import com.example.hexdemo.application.port.out.ImageGeneratorPort;
import com.example.hexdemo.domain.model.ProcessedMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessMessageUseCase processMessageUseCase;

    @MockBean
    private GetProcessedMessagesQuery getProcessedMessagesQuery;

    @MockBean
    private ImageGeneratorPort imageGeneratorPort;

    // ── GET /api/messages ──────────────────────────────────────────────────────

    @Test
    void get_messages_returns_200_with_empty_list() throws Exception {
        when(getProcessedMessagesQuery.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void get_messages_returns_all_processed_messages() throws Exception {
        ProcessedMessage msg = new ProcessedMessage(
                "id-1", "carrot", "ascii-art",
                ProcessedMessage.MessageSource.REST, Instant.parse("2026-01-01T00:00:00Z")
        );
        when(getProcessedMessagesQuery.getAll()).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("id-1"))
                .andExpect(jsonPath("$[0].messageType").value("carrot"))
                .andExpect(jsonPath("$[0].asciiArt").value("ascii-art"));
    }

    // ── POST /api/messages ─────────────────────────────────────────────────────

    @Test
    void post_message_returns_202_accepted() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("rabbit"))
                .andExpect(status().isAccepted());
    }

    @Test
    void post_message_returns_id_and_queued_status() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("rabbit"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("queued"));
    }

    @Test
    void post_message_delegates_to_use_case() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("carrot"));

        verify(processMessageUseCase).processMessage(any());
    }

    @Test
    void post_message_strips_whitespace_before_delegating() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("  carrot  "));

        verify(processMessageUseCase).processMessage(argThat(m -> "carrot".equals(m.content())));
    }

    // ── GET /api/plugins ───────────────────────────────────────────────────────

    @Test
    void get_plugins_returns_200() throws Exception {
        when(imageGeneratorPort.getSupportedTypes()).thenReturn(Set.of());

        mockMvc.perform(get("/api/plugins"))
                .andExpect(status().isOk());
    }

    @Test
    void get_plugins_returns_count_and_supported_types() throws Exception {
        when(imageGeneratorPort.getSupportedTypes()).thenReturn(Set.of("carrot", "rabbit"));

        mockMvc.perform(get("/api/plugins"))
                .andExpect(jsonPath("$.loaded").value(2))
                .andExpect(jsonPath("$.supportedTypes", containsInAnyOrder("carrot", "rabbit")));
    }

    @Test
    void get_plugins_reflects_zero_when_no_plugins_loaded() throws Exception {
        when(imageGeneratorPort.getSupportedTypes()).thenReturn(Set.of());

        mockMvc.perform(get("/api/plugins"))
                .andExpect(jsonPath("$.loaded").value(0))
                .andExpect(jsonPath("$.supportedTypes", empty()));
    }
}
