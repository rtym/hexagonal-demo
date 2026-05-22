package com.example.hexdemo.plugin;

/**
 * Contract every pluggable image-generator must satisfy.
 *
 * Implementations are discovered at runtime via {@link java.util.ServiceLoader}.
 * Each plugin JAR must contain:
 *   META-INF/services/com.example.hexdemo.plugin.ImageGeneratorPlugin
 * listing the fully-qualified implementation class name.
 *
 * Plugin JARs are hot-loaded from the {@code plugins/} directory — no application
 * restart is required when a new JAR is dropped in.
 */
public interface ImageGeneratorPlugin {

    /**
     * Returns true when this plugin can handle the given message type
     * (e.g. "carrot", "rabbit", "cabbage").
     */
    boolean supports(String messageType);

    /**
     * Generates and returns a multi-line ASCII-art image for the given type.
     */
    String generate(String messageType);

    /** Human-readable plugin name shown in the /api/plugins endpoint. */
    default String getPluginName() {
        return getClass().getSimpleName();
    }
}
