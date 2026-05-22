package com.example.hexdemo.adapter.out.memory;

import com.example.hexdemo.domain.model.ProcessedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMessageStorageAdapterTest {

    private InMemoryMessageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryMessageStorageAdapter();
    }

    @Test
    void findAll_returns_empty_list_initially() {
        assertThat(adapter.findAll()).isEmpty();
    }

    @Test
    void save_then_findAll_returns_saved_message() {
        adapter.save(message("id-1", "carrot"));

        List<ProcessedMessage> all = adapter.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).id()).isEqualTo("id-1");
    }

    @Test
    void findAll_returns_newest_first() {
        adapter.save(message("first", "carrot"));
        adapter.save(message("second", "rabbit"));
        adapter.save(message("third", "cabbage"));

        List<ProcessedMessage> all = adapter.findAll();
        assertThat(all).extracting(ProcessedMessage::id)
                .containsExactly("third", "second", "first");
    }

    @Test
    void save_returns_immutable_snapshot_from_findAll() {
        adapter.save(message("id-1", "carrot"));
        List<ProcessedMessage> snapshot = adapter.findAll();

        adapter.save(message("id-2", "rabbit"));

        assertThat(snapshot).hasSize(1);
    }

    @Test
    void store_is_bounded_to_200_entries() {
        for (int i = 0; i < 210; i++) {
            adapter.save(message("id-" + i, "carrot"));
        }

        assertThat(adapter.findAll()).hasSize(200);
    }

    @Test
    void bounded_store_retains_newest_entries() {
        for (int i = 0; i < 205; i++) {
            adapter.save(message("id-" + i, "carrot"));
        }
        // id-204 was saved last → should be first in the list
        assertThat(adapter.findAll().get(0).id()).isEqualTo("id-204");
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static ProcessedMessage message(String id, String type) {
        return new ProcessedMessage(id, type, "art", ProcessedMessage.MessageSource.FILE, Instant.now());
    }
}
