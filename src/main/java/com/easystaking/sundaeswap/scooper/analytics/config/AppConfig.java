package com.easystaking.sundaeswap.scooper.analytics.config;

import lombok.Getter;
import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.conversions.ClasspathConversionsFactory;
import org.cardanofoundation.conversions.domain.NetworkType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class AppConfig {

    @Component
    @Getter
    public static class CardanoNodeConfig {

        @Value("${cardano.node.host}")
        private String cardanoNodeHost;

        @Value("${cardano.node.port}")
        private Integer cardanoNodePort;

    }

    @Bean
    public CardanoConverters cardanoConverters() {
        return ClasspathConversionsFactory.createConverters(NetworkType.MAINNET);
    }

}
