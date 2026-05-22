package com.example.hexdemo.application.port.out;

import java.util.Optional;
import java.util.Set;

/**
 * Secondary (driven) port — the application core asks for an image via this
 * interface; the PluginRegistry implements it by delegating to whichever
 * {@link com.example.hexdemo.plugin.ImageGeneratorPlugin} was hot-loaded.
 */
public interface ImageGeneratorPort {
    Optional<String> generateImage(String messageType);
    Set<String> getSupportedTypes();
}
