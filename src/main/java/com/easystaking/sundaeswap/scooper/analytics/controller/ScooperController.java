package com.easystaking.sundaeswap.scooper.analytics.controller;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.easystaking.sundaeswap.scooper.analytics.model.ScooperBalance;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import com.easystaking.sundaeswap.scooper.analytics.service.ScooperBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/scoopers")
@RequiredArgsConstructor
@Slf4j
public class ScooperController {

    private final ScoopRepository scoopRepository;

    private final ScooperBalanceService getScooperStats;

    @GetMapping("/balances")
    public ResponseEntity<List<ScooperBalance>> getBalances() {

        var scooperBalances = getScooperStats.getScooperBalances()
                .stream()
                .map(pair -> {
                    var lastScoop = scoopRepository.findFirstByScooperPubKeyHashOrderBySlotDesc(pair.first())
                            .map(scoop -> scoop.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli())
                            .orElse(null);
                    return ScooperBalance.from(pair, pubKeyHash -> AddressProvider.getEntAddress(Credential.fromKey(pubKeyHash), Networks.mainnet()).getAddress(), lastScoop);
                })
                .toList();

        return ResponseEntity.ok(scooperBalances);
    }


}
