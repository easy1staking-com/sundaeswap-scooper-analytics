package com.easystaking.sundaeswap.scooper.analytics.entity.projections;

import java.time.LocalDateTime;

/**
 * Projection interface for protocol-level time-series analytics.
 * Represents aggregated metrics for a specific time period.
 */
public interface ProtocolTimeSeriesStats {

    /**
     * The start timestamp of the period (truncated to period boundary)
     */
    LocalDateTime getPeriod();

    /**
     * Total number of scoops in this period
     */
    Long getTotalScoops();

    /**
     * Sum of all orders in this period
     */
    Long getTotalOrders();

    /**
     * Sum of all protocol fees in this period (in lovelace)
     */
    Long getTotalProtocolFee();

    /**
     * Sum of all transaction fees in this period (in lovelace)
     */
    Long getTotalTransactionFee();
}
