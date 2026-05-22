package com.example.hexdemo.plugin.bundle;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;
import com.example.hexdemo.plugin.bundle.v1.CarrotPluginV1;
import com.example.hexdemo.plugin.bundle.v1.CabbagePluginV1;
import com.example.hexdemo.plugin.bundle.v1.RabbitPluginV1;
import com.example.hexdemo.plugin.bundle.v2.CarrotPluginV2;
import com.example.hexdemo.plugin.bundle.v2.CabbagePluginV2;
import com.example.hexdemo.plugin.bundle.v2.RabbitPluginV2;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BundlePluginTest {

    // ─── V1 ───────────────────────────────────────────────────────────────────

    @Nested
    class CarrotV1 {
        private final ImageGeneratorPlugin plugin = new CarrotPluginV1();

        @Test void supports_carrot() {
            assertThat(plugin.supports("carrot")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"rabbit", "cabbage", "", "CARROT", "Carrot"})
        void does_not_support_other_types(String type) {
            assertThat(plugin.supports(type)).isFalse();
        }

        @Test void generate_returns_non_empty_art() {
            assertThat(plugin.generate("carrot")).isNotBlank();
        }

        @Test void generate_contains_carrot_label() {
            assertThat(plugin.generate("carrot")).containsIgnoringCase("carrot");
        }

        @Test void plugin_name_includes_v1() {
            assertThat(plugin.getPluginName()).contains("v1");
        }
    }

    @Nested
    class RabbitV1 {
        private final ImageGeneratorPlugin plugin = new RabbitPluginV1();

        @Test void supports_rabbit() {
            assertThat(plugin.supports("rabbit")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"carrot", "cabbage", "", "RABBIT"})
        void does_not_support_other_types(String type) {
            assertThat(plugin.supports(type)).isFalse();
        }

        @Test void generate_returns_non_empty_art() {
            assertThat(plugin.generate("rabbit")).isNotBlank();
        }

        @Test void generate_contains_rabbit_label() {
            assertThat(plugin.generate("rabbit")).containsIgnoringCase("rabbit");
        }

        @Test void plugin_name_includes_v1() {
            assertThat(plugin.getPluginName()).contains("v1");
        }
    }

    @Nested
    class CabbageV1 {
        private final ImageGeneratorPlugin plugin = new CabbagePluginV1();

        @Test void supports_cabbage() {
            assertThat(plugin.supports("cabbage")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"carrot", "rabbit", "", "CABBAGE"})
        void does_not_support_other_types(String type) {
            assertThat(plugin.supports(type)).isFalse();
        }

        @Test void generate_returns_non_empty_art() {
            assertThat(plugin.generate("cabbage")).isNotBlank();
        }

        @Test void generate_contains_cabbage_label() {
            assertThat(plugin.generate("cabbage")).containsIgnoringCase("cabbage");
        }

        @Test void plugin_name_includes_v1() {
            assertThat(plugin.getPluginName()).contains("v1");
        }
    }

    // ─── V2 ───────────────────────────────────────────────────────────────────

    @Nested
    class CarrotV2 {
        private final ImageGeneratorPlugin plugin = new CarrotPluginV2();

        @Test void supports_carrot() {
            assertThat(plugin.supports("carrot")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"rabbit", "cabbage", "", "CARROT"})
        void does_not_support_other_types(String type) {
            assertThat(plugin.supports(type)).isFalse();
        }

        @Test void generate_returns_non_empty_art() {
            assertThat(plugin.generate("carrot")).isNotBlank();
        }

        @Test void generate_contains_carrot_label() {
            assertThat(plugin.generate("carrot")).containsIgnoringCase("carrot");
        }

        @Test void plugin_name_includes_v2() {
            assertThat(plugin.getPluginName()).contains("v2");
        }
    }

    @Nested
    class RabbitV2 {
        private final ImageGeneratorPlugin plugin = new RabbitPluginV2();

        @Test void supports_rabbit() {
            assertThat(plugin.supports("rabbit")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"carrot", "cabbage", "", "RABBIT"})
        void does_not_support_other_types(String type) {
            assertThat(plugin.supports(type)).isFalse();
        }

        @Test void generate_returns_non_empty_art() {
            assertThat(plugin.generate("rabbit")).isNotBlank();
        }

        @Test void generate_contains_rabbit_label() {
            assertThat(plugin.generate("rabbit")).containsIgnoringCase("rabbit");
        }

        @Test void plugin_name_includes_v2() {
            assertThat(plugin.getPluginName()).contains("v2");
        }
    }

    @Nested
    class CabbageV2 {
        private final ImageGeneratorPlugin plugin = new CabbagePluginV2();

        @Test void supports_cabbage() {
            assertThat(plugin.supports("cabbage")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"carrot", "rabbit", "", "CABBAGE"})
        void does_not_support_other_types(String type) {
            assertThat(plugin.supports(type)).isFalse();
        }

        @Test void generate_returns_non_empty_art() {
            assertThat(plugin.generate("cabbage")).isNotBlank();
        }

        @Test void generate_contains_cabbage_label() {
            assertThat(plugin.generate("cabbage")).containsIgnoringCase("cabbage");
        }

        @Test void plugin_name_includes_v2() {
            assertThat(plugin.getPluginName()).contains("v2");
        }
    }

    // ─── Cross-version contract ────────────────────────────────────────────────

    @Nested
    class CrossVersion {

        @Test void carrot_v1_and_v2_produce_different_art() {
            String artV1 = new CarrotPluginV1().generate("carrot");
            String artV2 = new CarrotPluginV2().generate("carrot");
            assertThat(artV1).isNotEqualTo(artV2);
        }

        @Test void rabbit_v1_and_v2_produce_different_art() {
            String artV1 = new RabbitPluginV1().generate("rabbit");
            String artV2 = new RabbitPluginV2().generate("rabbit");
            assertThat(artV1).isNotEqualTo(artV2);
        }

        @Test void cabbage_v1_and_v2_produce_different_art() {
            String artV1 = new CabbagePluginV1().generate("cabbage");
            String artV2 = new CabbagePluginV2().generate("cabbage");
            assertThat(artV1).isNotEqualTo(artV2);
        }

        @Test void v1_plugin_names_are_distinct_from_v2() {
            assertThat(new CarrotPluginV1().getPluginName())
                    .isNotEqualTo(new CarrotPluginV2().getPluginName());
            assertThat(new RabbitPluginV1().getPluginName())
                    .isNotEqualTo(new RabbitPluginV2().getPluginName());
            assertThat(new CabbagePluginV1().getPluginName())
                    .isNotEqualTo(new CabbagePluginV2().getPluginName());
        }

        @Test void all_plugins_implement_the_contract_interface() {
            assertThat(new CarrotPluginV1()).isInstanceOf(ImageGeneratorPlugin.class);
            assertThat(new CarrotPluginV2()).isInstanceOf(ImageGeneratorPlugin.class);
            assertThat(new RabbitPluginV1()).isInstanceOf(ImageGeneratorPlugin.class);
            assertThat(new RabbitPluginV2()).isInstanceOf(ImageGeneratorPlugin.class);
            assertThat(new CabbagePluginV1()).isInstanceOf(ImageGeneratorPlugin.class);
            assertThat(new CabbagePluginV2()).isInstanceOf(ImageGeneratorPlugin.class);
        }
    }
}
