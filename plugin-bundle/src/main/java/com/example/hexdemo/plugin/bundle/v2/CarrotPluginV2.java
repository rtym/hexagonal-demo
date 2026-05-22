package com.example.hexdemo.plugin.bundle.v2;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;

public class CarrotPluginV2 implements ImageGeneratorPlugin {

    @Override
    public boolean supports(String messageType) {
        return "carrot".equals(messageType);
    }

    @Override
    public String generate(String messageType) {
        return """
                         , , ,
                        ,,,,,,,
                       ,,,,,,,,,
                      ,,,,,,,,,,,
                    _______________
                   /               \\
                  |  CARROT  v2.0  |
                  |  *  *  *  *  * |
                   \\_____________/
                        | | |
                        | | |
                        |_|_|
                """;
    }

    @Override
    public String getPluginName() {
        return "CarrotPlugin v2";
    }
}
