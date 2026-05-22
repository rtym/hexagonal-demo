package com.example.hexdemo.infrastructure.plugin;

import com.example.hexdemo.application.port.out.ImageGeneratorPort;
import com.example.hexdemo.plugin.ImageGeneratorPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implements {@link ImageGeneratorPort} and acts as the single registry for all
 * hot-loaded plugins.
 *
 * Thread-safe: plugins can be registered at any time by the {@link PluginWatcher}
 * while the application is processing messages.
 *
 * The registry maps each supported message type directly to its plugin instance so
 * look-up is O(1) regardless of how many plugins are loaded.
 */
@Slf4j
@Component
public class PluginRegistry implements ImageGeneratorPort {

    private final ConcurrentMap<String, ImageGeneratorPlugin> byType = new ConcurrentHashMap<>();

    /**
     * Called by {@link PluginWatcher} whenever a new JAR is loaded.
     * A plugin may support multiple types; each is registered separately.
     */
    public void register(ImageGeneratorPlugin plugin) {
        // Probe all three known types; plugins declare support via supports()
        for (String candidate : new String[]{"carrot", "rabbit", "cabbage"}) {
            if (plugin.supports(candidate)) {
                byType.put(candidate, plugin);
                log.info("Plugin registered: '{}' → {}", candidate, plugin.getPluginName());
            }
        }
    }

    @Override
    public Optional<String> generateImage(String messageType) {
        ImageGeneratorPlugin plugin = byType.get(messageType);
        if (plugin == null) return Optional.empty();
        return Optional.of(plugin.generate(messageType));
    }

    @Override
    public Set<String> getSupportedTypes() {
        return Set.copyOf(byType.keySet());
    }
}
