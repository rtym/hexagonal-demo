package com.example.hexdemo.plugin.bundle.v1;

import com.example.hexdemo.plugin.ImageGeneratorPlugin;

public class RabbitPluginV1 implements ImageGeneratorPlugin {

    @Override
    public boolean supports(String messageType) {
        return "rabbit".equals(messageType);
    }

    @Override
    public String generate(String messageType) {
        return """
                   /\\ /\\
                  ( o.o )
                  > ^ <
                 (  ___  )
                /|       |\\
               / |  ( )  | \\
                  |_____|
                  RABBIT
                """;
    }

    @Override
    public String getPluginName() {
        return "RabbitPlugin v1";
    }
}
