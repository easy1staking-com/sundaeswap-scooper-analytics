package com.easystaking.sundaeswap.scooper.analytics.controller;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.model.ExtendedScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.model.ProtocolScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.model.ScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import com.easystaking.sundaeswap.scooper.analytics.service.SlotConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

@RestController
@RequestMapping("/scoops")
@AllArgsConstructor
@Slf4j
public class ScoopController {

    private final ScoopRepository scoopRepository;

    private final SlotConversionService slotConversionService;

    @GetMapping
    public ResponseEntity<List<Scoop>> get(@RequestParam(required = false) String scooperPubKeyHash,
                                           @RequestParam(required = false) Long slot,
                                           @RequestParam(required = false, defaultValue = "25") Integer limit) {
        log.info("scooperPubKeyHash: {}", scooperPubKeyHash);
        Long actualSlot = requireNonNullElse(slot, Long.MAX_VALUE);
        List<Scoop> scoops;
        if (scooperPubKeyHash == null || scooperPubKeyHash.isBlank()) {
            scoops = scoopRepository.findAllBySlotLessThanOrderBySlotDesc(actualSlot, Limit.of(limit));
        } else {
            scoops = scoopRepository.findAllByScooperPubKeyHashAndSlotLessThanOrderBySlotDesc(scooperPubKeyHash, actualSlot, Limit.of(limit));
        }
        return ResponseEntity.ok(scoops);
    }

    @GetMapping("/{txHash}")
    public ResponseEntity<Scoop> getByTxHash(@PathVariable String txHash) {
        log.info("txHash: {}", txHash);
        var scoopOpt = scoopRepository.findById(txHash);
        return scoopOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ProtocolScooperStats protocolScooperStats(Supplier<List<ScooperStats>> scooperStatsSupplier, Long limit) {

        var actualLimit = limit != null ? limit : Long.MAX_VALUE;

        List<ScooperStats> scooperStats = scooperStatsSupplier.get();

        var sortedStats = scooperStats.stream()
                .sorted(Comparator.comparingLong(ScooperStats::getTotalScoops).reversed())
                .map(stats -> ExtendedScooperStats.from(stats, pkh -> AddressProvider.getEntAddress(Credential.fromKey(pkh), Networks.mainnet()).getAddress()))
                .limit(actualLimit)
                .collect(Collectors.toList());

        var totalScoops = sortedStats.stream().map(ExtendedScooperStats::totalScoops).reduce(Long::sum).orElse(0L);
        var totalOrders = sortedStats.stream().map(ExtendedScooperStats::totalOrders).reduce(Long::sum).orElse(0L);
        var totalProtocolFee = sortedStats.stream().map(ExtendedScooperStats::totalProtocolFee).reduce(Long::sum).orElse(0L);
        var totalTransactionFee = sortedStats.stream().map(ExtendedScooperStats::totalTransactionFee).reduce(Long::sum).orElse(0L);

        return new ProtocolScooperStats(totalScoops, totalOrders, totalProtocolFee, totalTransactionFee, sortedStats);

    }


    @GetMapping("/stats")
    public ResponseEntity<ProtocolScooperStats> getStats(@RequestParam(required = false) Integer limit,
                                                         @RequestParam(required = false) Long epoch) {

        var actualLimit = limit != null ? limit : Long.MAX_VALUE;

        ProtocolScooperStats protocolScooperStats;
        if (epoch == null) {
            protocolScooperStats = protocolScooperStats(scoopRepository::findScooperStats, actualLimit);
        } else {
            protocolScooperStats = protocolScooperStats(() -> scoopRepository.findScooperStatsByEpoch(epoch), actualLimit);

        }

        return ResponseEntity.ok(protocolScooperStats);

    }

    @Operation(description = "Provide recent scooper statistics")
    @GetMapping("/stats/{duration}")
    public ResponseEntity<ProtocolScooperStats> getRecentStats(
            @Parameter(description = "The duration of the recent statistics. Refer to [docs.oracle.com](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-) for full docs",
                    example = "PT5M for the last 5 minutes")
            @PathVariable String duration,
            @RequestParam(required = false) Integer limit) {

        var actualDuration = Duration.parse(duration);
        log.info("actualDuration: {}", actualDuration);

        var actualLimit = limit != null ? limit : Long.MAX_VALUE;

        var slotFrom = slotConversionService.toSlotFromNow(now -> now.minusMinutes(actualDuration.toMinutes()));

        ProtocolScooperStats protocolScooperStats = protocolScooperStats(() -> scoopRepository.findScooperStatsFromSlot(slotFrom), actualLimit);

        return ResponseEntity.ok(protocolScooperStats);

    }

    @Operation(description = "Provide scoopers statistics for a given month")
    @GetMapping("/stats/{year}/{month}")
    public ResponseEntity<ProtocolScooperStats> getGivenMonthStats(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(required = false) Long limit) {

        var date = LocalDate.of(year, month, 1).atStartOfDay();

        var slotFrom = slotConversionService.toSlot(date);
        var slotTo = slotConversionService.toSlot(date.plusMonths(1));

        ProtocolScooperStats protocolScooperStats = protocolScooperStats(() -> scoopRepository.findScooperStatsBetweenSlots(slotFrom, slotTo), limit);

        return ResponseEntity.ok(protocolScooperStats);

    }

    @Operation(description = "Provide scoopers statistics for a given day")
    @GetMapping("/stats/{year}/{month}/{day}")
    public ResponseEntity<ProtocolScooperStats> getGivenDayStats(
            @PathVariable int year,
            @PathVariable int month,
            @PathVariable int day,
            @RequestParam(required = false) Long limit) {

        var date = LocalDate.of(year, month, day).atStartOfDay();

        var slotFrom = slotConversionService.toSlot(date);
        var slotTo = slotConversionService.toSlot(date.plusDays(1));

        ProtocolScooperStats protocolScooperStats = protocolScooperStats(() -> scoopRepository.findScooperStatsBetweenSlots(slotFrom, slotTo), limit);

        return ResponseEntity.ok(protocolScooperStats);

    }

}
