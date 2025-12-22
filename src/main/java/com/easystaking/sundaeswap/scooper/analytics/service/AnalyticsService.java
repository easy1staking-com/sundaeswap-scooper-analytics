package com.easystaking.sundaeswap.scooper.analytics.service;

import com.easystaking.sundaeswap.scooper.analytics.model.ProtocolPeriodStats;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ScoopRepository scoopRepository;

    /**
     * Get protocol analytics for a date range with specified granularity.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (exclusive)
     * @param granularity 'day', 'week', 'month', 'hour'
     * @return Time-series data
     */
    public List<ProtocolPeriodStats> getProtocolTimeSeries(
            LocalDate startDate,
            LocalDate endDate,
            String granularity) {

        var startDateTime = startDate.atStartOfDay();
        var endDateTime = endDate.atStartOfDay();

        log.info("Fetching {} analytics from {} to {}",
            granularity, startDateTime, endDateTime);

        var stats = scoopRepository.findProtocolTimeSeriesStats(
            startDateTime,
            endDateTime,
            granularity);

        return stats.stream()
            .map(ProtocolPeriodStats::from)
            .toList();
    }

    /**
     * Get protocol analytics grouped by epoch.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (exclusive)
     * @return Time-series data grouped by epoch
     */
    public List<ProtocolPeriodStats> getProtocolEpochStats(
            LocalDate startDate,
            LocalDate endDate) {

        var startDateTime = startDate.atStartOfDay();
        var endDateTime = endDate.atStartOfDay();

        log.info("Fetching epoch analytics from {} to {}",
            startDateTime, endDateTime);

        var stats = scoopRepository.findProtocolEpochStats(
            startDateTime,
            endDateTime);

        return stats.stream()
            .map(ProtocolPeriodStats::from)
            .toList();
    }
}
