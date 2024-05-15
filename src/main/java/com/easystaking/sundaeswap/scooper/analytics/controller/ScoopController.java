package com.easystaking.sundaeswap.scooper.analytics.controller;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.model.ExtendedScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.model.ProtocolScooperStats;
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
    public ResponseEntity<ProtocolScooperStats> getStats(@RequestParam(required = false) Integer limit,
                                                         @RequestParam(required = false) Long epoch) {
        var actualLimit = limit != null ? limit : Long.MAX_VALUE;
        List<ScooperStats> scooperStats = epoch != null ? scoopRepository.findScooperStatsByEpoch(epoch) : scoopRepository.findScooperStats();

        var sortedStats = scooperStats.stream()
                .sorted(Comparator.comparingLong(ScooperStats::getTotalScoops).reversed())
                .map(stats -> ExtendedScooperStats.from(stats, pkh -> AddressProvider.getEntAddress(Credential.fromKey(pkh), Networks.mainnet()).getAddress()))
                .limit(actualLimit)
                .collect(Collectors.toList());

        var totalScoops = sortedStats.stream().map(ExtendedScooperStats::totalScoops).reduce(Long::sum).orElse(0L);
        var totalOrders = sortedStats.stream().map(ExtendedScooperStats::totalOrders).reduce(Long::sum).orElse(0L);
        var totalUserFee = sortedStats.stream().map(ExtendedScooperStats::totalUserFee).reduce(Long::sum).orElse(0L);
        var totalTransactionFee = sortedStats.stream().map(ExtendedScooperStats::totalTransactionFee).reduce(Long::sum).orElse(0L);

        ProtocolScooperStats protocolScooperStats = new ProtocolScooperStats(totalScoops, totalOrders, totalUserFee, totalTransactionFee, sortedStats);

        return ResponseEntity.ok(protocolScooperStats);
    }

}
