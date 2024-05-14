package com.easystaking.sundaeswap.scooper.analytics.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class AppConfig {

    @Component
    @Getter
    public static class BlockStreamerConfig {

        @Value("${block-streamer.host}")
        private String blockStreamerHost;

        @Value("${block-streamer.port}")
        private Integer blockStreamerPort;

    }

}
