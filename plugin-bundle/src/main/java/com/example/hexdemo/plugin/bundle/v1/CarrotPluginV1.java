package com.example.hexdemo.plugin.bundle.v1;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;

public class CarrotPluginV1 implements ImageGeneratorPlugin {

    @Override
    public boolean supports(String messageType) {
        return "carrot".equals(messageType);
    }

    @Override
    public String generate(String messageType) {
        return """
                      ,//
                     />//
                    //>//
                   //>|//
                  // >|>//
                 // >||>//
                ///>|||>//
              __||||||||||__
             (   CARROT    )
              \\___________/
                  |   |
                  |   |
                   \\_/
                """;
    }

    @Override
    public String getPluginName() {
        return "CarrotPlugin v1";
    }
}
