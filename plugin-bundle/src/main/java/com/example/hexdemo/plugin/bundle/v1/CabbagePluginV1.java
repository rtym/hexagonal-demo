package com.example.hexdemo.plugin.bundle.v1;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;

public class CabbagePluginV1 implements ImageGeneratorPlugin {

    @Override
    public boolean supports(String messageType) {
        return "cabbage".equals(messageType);
    }

    @Override
    public String generate(String messageType) {
        return """
                    .-""-.
                   /  . .  \\
                  | (     ) |
                  |  `~~~'  |
                   \\  vvv  /
                    `-----'
                   (CABBAGE)
                """;
    }

    @Override
    public String getPluginName() {
        return "CabbagePlugin v1";
    }
}
