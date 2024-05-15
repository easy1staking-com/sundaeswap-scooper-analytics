package com.easystaking.sundaeswap.scooper.analytics.config;

import lombok.Getter;
import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.conversions.ClasspathConversionsFactory;
import org.cardanofoundation.conversions.ConversionsConfig;
import org.cardanofoundation.conversions.domain.NetworkType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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

    @Bean
    public CardanoConverters cardanoConverters() {
        return ClasspathConversionsFactory.createConverters(NetworkType.MAINNET);
    }

}
