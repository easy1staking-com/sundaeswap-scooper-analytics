package com.easystaking.sundaeswap.scooper.analytics.config;

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class BlockfrostConfig {

    @Value("${blockfrost.url}")
    private String blockfrostUrl;

    @Value("${blockfrost.key}")
    private String blockfrostKey;

    @Bean
    public BFBackendService bfBackendService() {
        return new BFBackendService(blockfrostUrl, blockfrostKey);
    }

}
