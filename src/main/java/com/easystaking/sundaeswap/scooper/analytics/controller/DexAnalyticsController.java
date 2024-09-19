package com.easystaking.sundaeswap.scooper.analytics.controller;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.easystaking.sundaeswap.scooper.analytics.model.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/dex/analytics")
@RequiredArgsConstructor
@Slf4j
public class DexAnalyticsController {

    public record DexAnalytics(int numOfV3Pools, long totalValueLocked) {

    }

    private final BFBackendService bfBackendService;

    @GetMapping
    public ResponseEntity<?> get() throws Exception {

        List<Utxo> utxos = new ArrayList<>();

        var page = 1;
        var poolUtxos = bfBackendService.getUtxoService().getUtxos(Constants.POOL_ADDRESS_V2, 100, page++);

        while (poolUtxos.isSuccessful() && !poolUtxos.getValue().isEmpty()) {
            utxos.addAll(poolUtxos.getValue());
            poolUtxos = bfBackendService.getUtxoService().getUtxos(Constants.POOL_ADDRESS_V2, 100, page++);
        }

        var actualAmmPools = utxos
                .stream()
                .filter(utxo -> utxo.getAmount().stream().anyMatch(amount -> amount.getUnit().startsWith(Constants.POOL_NFT_POLICY_ID)))
                .toList();

        var adaTvl = actualAmmPools.stream()
                .flatMap(utxo -> utxo.getAmount().stream().filter(amount -> amount.getUnit().equals("lovelace")).findAny().stream())
                .map(Amount::getQuantity)
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO);


        return ResponseEntity.ok(new DexAnalytics(actualAmmPools.size(), adaTvl.longValue() * 2));


    }


}
