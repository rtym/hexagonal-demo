package com.example.hexdemo.infrastructure.plugin;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PluginRegistryTest {

    private PluginRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PluginRegistry();
    }

    @Test
    void empty_registry_returns_empty_optional() {
        assertThat(registry.generateImage("carrot")).isEmpty();
    }

    @Test
    void empty_registry_has_no_supported_types() {
        assertThat(registry.getSupportedTypes()).isEmpty();
    }

    @Test
    void register_maps_supported_type_to_plugin() {
        registry.register(stubPlugin("carrot", "art-carrot"));

        assertThat(registry.generateImage("carrot")).contains("art-carrot");
    }

    @Test
    void register_does_not_map_unsupported_types() {
        registry.register(stubPlugin("carrot", "art-carrot"));

        assertThat(registry.generateImage("rabbit")).isEmpty();
        assertThat(registry.generateImage("cabbage")).isEmpty();
    }

    @Test
    void getSupportedTypes_returns_all_registered_types() {
        registry.register(stubPlugin("carrot", "art-carrot"));
        registry.register(stubPlugin("rabbit", "art-rabbit"));

        assertThat(registry.getSupportedTypes()).containsExactlyInAnyOrder("carrot", "rabbit");
    }

    @Test
    void registering_new_plugin_for_same_type_replaces_old_one() {
        registry.register(stubPlugin("carrot", "v1-art"));
        registry.register(stubPlugin("carrot", "v2-art"));

        assertThat(registry.generateImage("carrot")).contains("v2-art");
    }

    @Test
    void registering_new_plugin_for_same_type_does_not_change_supported_types_count() {
        registry.register(stubPlugin("carrot", "v1-art"));
        registry.register(stubPlugin("carrot", "v2-art"));

        assertThat(registry.getSupportedTypes()).hasSize(1);
    }

    @Test
    void getSupportedTypes_returns_immutable_snapshot() {
        registry.register(stubPlugin("carrot", "art"));
        var snapshot = registry.getSupportedTypes();

        registry.register(stubPlugin("rabbit", "art"));

        // The snapshot taken before rabbit was registered must not change.
        assertThat(snapshot).containsExactly("carrot");
    }

    @Test
    void generateImage_returns_exactly_the_art_from_plugin() {
        String expected = "  /\\\n (O)\n";
        registry.register(stubPlugin("rabbit", expected));

        Optional<String> result = registry.generateImage("rabbit");
        assertThat(result).contains(expected);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private static ImageGeneratorPlugin stubPlugin(String type, String art) {
        return new ImageGeneratorPlugin() {
            @Override public boolean supports(String t) { return type.equals(t); }
            @Override public String generate(String t) { return art; }
            @Override public String getPluginName() { return "stub-" + type; }
        };
    }
}
