package com.easystaking.sundaeswap.scooper.analytics.config;

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class BlockfrostConfig {

    @Value("${blockfrost.url:#null}")
    private String blockfrostUrl;

    @Value("${blockfrost.key::#null}")
    private String blockfrostKey;

    @Bean
    public BFBackendService bfBackendService() {
        log.info("INIT using bf: {}", blockfrostUrl);
        return new BFBackendService(blockfrostUrl, blockfrostKey);
    }

//    @Bean
//    public AikenTransactionEvaluator aikenTransactionEvaluator(HybridUtxoSupplier hybridUtxoSupplier,
//                                                               BFBackendService bfBackendService,
//                                                               JpgstoreScriptProvider jpgstoreScriptProvider) {
//        return new AikenTransactionEvaluator(hybridUtxoSupplier,
//                new DefaultProtocolParamsSupplier(bfBackendService.getEpochService()),
//                jpgstoreScriptProvider);
//    }
}
