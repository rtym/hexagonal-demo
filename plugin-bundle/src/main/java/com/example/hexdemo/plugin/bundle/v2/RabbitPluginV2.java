package com.example.hexdemo.plugin.bundle.v2;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;

public class RabbitPluginV2 implements ImageGeneratorPlugin {

    @Override
    public boolean supports(String messageType) {
        return "rabbit".equals(messageType);
    }

    @Override
    public String generate(String messageType) {
        return """
                   (\\ /)  (\\ /)
                   ( ^.^)  (^.^ )
                   c(")(")(")(")
                   ________________
                  |  RABBIT  v2.0  |
                  |  (fluffy ears) |
                  |________________|
                        | |
                        | |
                """;
    }

    @Override
    public String getPluginName() {
        return "RabbitPlugin v2";
    }
}
