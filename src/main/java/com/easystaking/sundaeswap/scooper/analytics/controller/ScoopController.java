package com.easystaking.sundaeswap.scooper.analytics.controller;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.model.ExtendedScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.model.ScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

@RestController
@RequestMapping("/scoops")
@AllArgsConstructor
@Slf4j
public class ScoopController {

    private final ScoopRepository scoopRepository;

    @GetMapping
    public ResponseEntity<List<Scoop>> get(@RequestParam(required = false) Long slot,
                                           @RequestParam(required = false, defaultValue = "25") Integer limit) {
        Long actualSlot = requireNonNullElse(slot, Long.MAX_VALUE);
        List<Scoop> scoops = scoopRepository.findAllBySlotLessThanOrderBySlotDesc(actualSlot, Limit.of(limit));
        return ResponseEntity.ok(scoops);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<ExtendedScooperStats>> getStats() {
        List<ScooperStats> scooperStats = scoopRepository.findScooperStats();
        var sortedStats = scooperStats.stream()
                .sorted(Comparator.comparingLong(ScooperStats::getTotalScoops))
                .map(foo -> ExtendedScooperStats.from(foo, pkh -> AddressProvider.getEntAddress(Credential.fromKey(pkh), Networks.mainnet()).getAddress()))
                .collect(Collectors.toList())
                .reversed();
        return ResponseEntity.ok(sortedStats);
    }

}
