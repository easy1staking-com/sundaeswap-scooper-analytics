package com.easystaking.sundaeswap.scooper.analytics.model;

import com.easystaking.sundaeswap.scooper.analytics.entity.projections.ProtocolTimeSeriesStats;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.ZoneOffset;

/**
 * Response model for protocol-level time-series analytics.
 * Represents aggregated metrics for a single time period.
 * Period is returned as epoch milliseconds for API compatibility.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProtocolPeriodStats(
    Long period,
    Long scoops,
    Long orders,
    Long protocolFee,
    Long transactionFee
) {
    /**
     * Factory method to create from projection.
     * Converts LocalDateTime period to epoch milliseconds.
     */
    public static ProtocolPeriodStats from(ProtocolTimeSeriesStats projection) {
        return new ProtocolPeriodStats(
            projection.getPeriod().toInstant(ZoneOffset.UTC).toEpochMilli(),
            projection.getTotalScoops(),
            projection.getTotalOrders(),
            projection.getTotalProtocolFee(),
            projection.getTotalTransactionFee()
        );
    }
}
