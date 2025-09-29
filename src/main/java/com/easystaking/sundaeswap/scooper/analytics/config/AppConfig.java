package com.easystaking.sundaeswap.scooper.analytics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.conversions.CardanoConverters;
import org.cardanofoundation.conversions.ClasspathConversionsFactory;
import org.cardanofoundation.conversions.domain.NetworkType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public CardanoConverters cardanoConverters() {
        return ClasspathConversionsFactory.createConverters(NetworkType.MAINNET);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
