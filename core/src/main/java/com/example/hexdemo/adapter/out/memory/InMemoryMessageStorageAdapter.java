package com.example.hexdemo.adapter.out.memory;

import com.example.hexdemo.application.port.out.MessageStoragePort;
import com.example.hexdemo.domain.model.ProcessedMessage;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Driven adapter — keeps processed messages in a bounded in-memory deque.
 * Newest entries are prepended so {@link #findAll()} returns them newest-first
 * without sorting. Thread-safe: {@link ConcurrentLinkedDeque} is used so the
 * {@link com.example.hexdemo.infrastructure.plugin.PluginWatcher} hot-load
 * thread and the REST thread can access it concurrently.
 */
@Component
public class InMemoryMessageStorageAdapter implements MessageStoragePort {

    private static final int MAX_ENTRIES = 200;

    private final Deque<ProcessedMessage> store = new ConcurrentLinkedDeque<>();

    @Override
    public void save(ProcessedMessage message) {
        store.addFirst(message);
        while (store.size() > MAX_ENTRIES) {
            store.pollLast();
        }
    }

    @Override
    public List<ProcessedMessage> findAll() {
        return List.copyOf(store);
    }
}
