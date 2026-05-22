package com.example.hexdemo.plugin.bundle.v2;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;

public class CabbagePluginV2 implements ImageGeneratorPlugin {

    @Override
    public boolean supports(String messageType) {
        return "cabbage".equals(messageType);
    }

    @Override
    public String generate(String messageType) {
        return """
                     . ~ ~ ~ ~ ~ .
                    ( @ @ @ @ @ @ )
                   ( @  CABBAGE  @ )
                   ( @   v 2.0   @ )
                    ( @ @ @ @ @ @ )
                     . ~ ~ ~ ~ ~ .
                     [fresh batch]
                """;
    }

    @Override
    public String getPluginName() {
        return "CabbagePlugin v2";
    }
}
