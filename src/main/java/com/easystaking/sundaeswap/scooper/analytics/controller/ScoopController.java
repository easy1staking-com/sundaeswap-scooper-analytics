package com.easystaking.sundaeswap.scooper.analytics.controller;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.entity.projections.ScooperPeriodStats;
import com.easystaking.sundaeswap.scooper.analytics.model.ExtendedScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.model.PeriodType;
import com.easystaking.sundaeswap.scooper.analytics.model.ProtocolScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.model.ScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import com.easystaking.sundaeswap.scooper.analytics.service.ScooperService;
import com.easystaking.sundaeswap.scooper.analytics.service.SlotConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/scoops")
@AllArgsConstructor
@Slf4j
public class ScoopController {

    private final ScooperService scooperService;

    private final ScoopRepository scoopRepository;

    private final SlotConversionService slotConversionService;

    @GetMapping
    public ResponseEntity<List<Scoop>> get(@RequestParam(required = false, name = "scooper_pub_key_hash") String scooperPubKeyHash,
                                           @Parameter(description = "the start date, in ISO format, to calculate scoopers statistics", example = "2024-04-16")
                                           @RequestParam(required = false, name = "date_start") LocalDate dateStart,
                                           @Parameter(description = "the end date (excluded), in ISO format, to calculate scoopers statistics", example = "2024-06-24")
                                           @RequestParam(required = false, name = "date_end") LocalDate dateEnd,
                                           @Parameter(description = "The way scoops should be sorted based on slot number, accepted values are ASC and DESC, " +
                                                   "where ASC is the natural chronological order and DESC the reverse", example = "DESC")
                                           @RequestParam(required = false, defaultValue = "ASC") String sort,
                                           @RequestParam(required = false) Integer limit) {

        var actualLimit = limit == null ? Integer.MAX_VALUE : limit;
        log.info("date range, from: {}, to: {}", dateStart, dateEnd);

        var slotFrom = dateStart == null ? 0L : slotConversionService.toSlot(dateStart.atStartOfDay());
        var slotTo = dateEnd == null ? Long.MAX_VALUE : slotConversionService.toSlot(dateEnd.atStartOfDay());
        log.info("slot range, from: {}, to: {}", slotFrom, slotTo);

        Sort sortBy;
        if (sort != null && sort.equals("DESC")) {
            sortBy = Sort.by(Sort.Order.desc("slot"));
        } else {
            sortBy = Sort.by(Sort.Order.asc("slot"));
        }

        List<Scoop> scoops;
        if (scooperPubKeyHash == null || scooperPubKeyHash.isBlank()) {
            scoops = scoopRepository.findAllBySlotBetween(slotFrom, slotTo, sortBy, Limit.of(actualLimit));
        } else {
            scoops = scoopRepository.findAllByScooperPubKeyHashAndSlotBetween(scooperPubKeyHash, slotFrom, slotTo, sortBy, Limit.of(actualLimit));
        }
        return ResponseEntity.ok(scoops);
    }

    @GetMapping("/{txHash}")
    public ResponseEntity<Scoop> getByTxHash(@PathVariable String txHash) {
        log.info("txHash: {}", txHash);
        var scoopOpt = scoopRepository.findById(txHash);
        return scoopOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ProtocolScooperStats protocolScooperStats(Supplier<List<ScooperStats>> scooperStatsSupplier) {

        List<ScooperStats> scooperStats = scooperStatsSupplier.get();

        var sortedStats = scooperStats.stream()
                .sorted(Comparator.comparingLong(ScooperStats::getTotalScoops).reversed())
                .map(stats -> ExtendedScooperStats.from(stats, pkh -> AddressProvider.getEntAddress(Credential.fromKey(pkh), Networks.mainnet()).getAddress()))
                .collect(Collectors.toList());

        var totalScoops = sortedStats.stream().map(ExtendedScooperStats::totalScoops).reduce(Long::sum).orElse(0L);
        var totalOrders = sortedStats.stream().map(ExtendedScooperStats::totalOrders).reduce(Long::sum).orElse(0L);
        var totalProtocolFee = sortedStats.stream().map(ExtendedScooperStats::totalProtocolFee).reduce(Long::sum).orElse(0L);
        var totalTransactionFee = sortedStats.stream().map(ExtendedScooperStats::totalTransactionFee).reduce(Long::sum).orElse(0L);
        var totalNumMempoolOrders = sortedStats.stream().map(ExtendedScooperStats::totalNumMempoolOrders).reduce(Long::sum).orElse(0L);

        return new ProtocolScooperStats(totalScoops, totalOrders, totalProtocolFee, totalTransactionFee, totalNumMempoolOrders, sortedStats);

    }

    @Operation(description = "Provide recent scooper statistics")
    @GetMapping("/stats/scooper/{scooperPubKeyHash}")
    public ResponseEntity<List<ScooperPeriodStats>> getScooperPeriodStats(
            @Parameter(description = "The pub key hash of the scooper", example = "37eb116b3ff8a70e4be778b5e8d30d3b40421ffe6622f6a983f67f3f")
            @PathVariable String scooperPubKeyHash,
            @Parameter(description = "Period type, month, hour, day")
            @RequestParam(value = "period_type", required = false, defaultValue = "DAYS") String periodType,
            @Parameter(description = "Period length")
            @RequestParam(value = "period_length", required = false, defaultValue = "7") Integer periodLength) {

        var period = PeriodType.valueOf(periodType.toUpperCase());

        var stats = scooperService.getScooperStats(scooperPubKeyHash, periodLength, period);

        return ResponseEntity.ok(stats);

    }

    @Operation(description = "Provide recent scooper statistics")
    @GetMapping("/stats/{duration}")
    public ResponseEntity<ProtocolScooperStats> getRecentStats(
            @Parameter(description = "The duration of the recent statistics. Refer to [docs.oracle.com](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-) for full docs",
                    example = "PT5M for the last 5 minutes")
            @PathVariable String duration) {

        var actualDuration = Duration.parse(duration);
        log.info("actualDuration: {}", actualDuration);


        var slotFrom = slotConversionService.toSlotFromNow(now -> now.minusMinutes(actualDuration.toMinutes()));

        ProtocolScooperStats protocolScooperStats = protocolScooperStats(() -> scoopRepository.findScooperStatsFromSlot(slotFrom));

        return ResponseEntity.ok(protocolScooperStats);

    }

    @Operation(summary = "Provide scoopers statistics", description = "Provide per scooper all time scooper stats. Time interval can be reduced by specifying either an epoch, or any combination of start and end date.")
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@Parameter(description = "the start date, in ISO format, to calculate scoopers statistics", example = "2024-04-16")
                                      @RequestParam(required = false, name = "date_start") LocalDate dateStart,
                                      @Parameter(description = "the end date (excluded), in ISO format, to calculate scoopers statistics", example = "2024-06-24")
                                      @RequestParam(required = false, name = "date_end") LocalDate dateEnd,
                                      @Parameter(description = "the epoch for which calculating stats", example = "324")
                                      @RequestParam(required = false) Long epoch) {

        log.info("dateStart: {}", dateStart);
        log.info("dateEnd: {}", dateEnd);
        log.info("epoch: {}", epoch);

        ProtocolScooperStats protocolScooperStats;
        if (epoch != null && (dateStart != null || dateEnd != null)) {
            return ResponseEntity.badRequest().body("Cannot mix up epoch and date range filtering");
        } else if (epoch != null) {
            protocolScooperStats = protocolScooperStats(() -> scoopRepository.findScooperStatsByEpoch(epoch));
        } else {
            var slotFrom = dateStart == null ? 0L : slotConversionService.toSlot(dateStart.atStartOfDay());
            var slotTo = dateEnd == null ? Long.MAX_VALUE : slotConversionService.toSlot(dateEnd.atStartOfDay());
            protocolScooperStats = protocolScooperStats(() -> scoopRepository.findScooperStatsBetweenSlots(slotFrom, slotTo));
        }

        return ResponseEntity.ok(protocolScooperStats);

    }

}
