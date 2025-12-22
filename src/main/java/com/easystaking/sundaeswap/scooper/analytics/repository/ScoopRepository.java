package com.easystaking.sundaeswap.scooper.analytics.repository;

import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.entity.projections.ProtocolTimeSeriesStats;
import com.easystaking.sundaeswap.scooper.analytics.entity.projections.ScooperPeriodStats;
import com.easystaking.sundaeswap.scooper.analytics.model.ScooperStats;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScoopRepository extends JpaRepository<Scoop, String> {

    //V2
    List<Scoop> findAllBySlotBetween(Long slotFrom, Long slotTo, Sort sort, Limit limit);

    List<Scoop> findAllByScooperPubKeyHashAndSlotBetween(String scooperPubKeyHash, Long slotFrom, Long slotTo, Sort sort, Limit limit);

    @Query("SELECT scooperPubKeyHash AS pubKeyHash, count(1) AS totalScoops, sum(orders) AS totalOrders, " +
            "sum(protocolFee) as totalProtocolFee, sum(transactionFee) as totalTransactionFee, sum(numMempoolOrders) AS totalNumMempoolOrders " +
            "FROM Scoop GROUP BY scooperPubKeyHash")
    List<ScooperStats> findScooperStats();

    @Query("SELECT scooperPubKeyHash AS pubKeyHash, count(1) AS totalScoops, sum(orders) AS totalOrders, " +
            "sum(protocolFee) as totalProtocolFee, sum(transactionFee) as totalTransactionFee, sum(numMempoolOrders) AS totalNumMempoolOrders " +
            "FROM Scoop " +
            "WHERE epoch = :epoch " +
            "GROUP BY scooperPubKeyHash, epoch ")
    List<ScooperStats> findScooperStatsByEpoch(Long epoch);

    @Query("SELECT scooperPubKeyHash AS pubKeyHash, count(1) AS totalScoops, sum(orders) AS totalOrders, " +
            "sum(protocolFee) as totalProtocolFee, sum(transactionFee) as totalTransactionFee, sum(numMempoolOrders) AS totalNumMempoolOrders " +
            "FROM Scoop " +
            "WHERE slot > :slot " +
            "GROUP BY scooperPubKeyHash ")
    List<ScooperStats> findScooperStatsFromSlot(Long slot);

    @Query("SELECT scooperPubKeyHash AS pubKeyHash, count(1) AS totalScoops, sum(orders) AS totalOrders, " +
            "sum(protocolFee) as totalProtocolFee, sum(transactionFee) as totalTransactionFee, sum(numMempoolOrders) AS totalNumMempoolOrders " +
            "FROM Scoop " +
            "WHERE slot between :slotFrom and :slotTo " +
            "GROUP BY scooperPubKeyHash ")
    List<ScooperStats> findScooperStatsBetweenSlots(Long slotFrom, Long slotTo);

    Optional<Scoop> findAllByOrderBySlotDesc(Limit limit);

    Long deleteBySlotGreaterThan(Long slot);


    @Query("SELECT ((slot - :slotTo) / :periodLength) AS period, SUM ( CASE WHEN scooperPubKeyHash = :scooperPubKeyHash THEN 1 ELSE 0 END ) AS scooperNumberScoops, " +
            "count(1)  AS totalNumberScoops FROM Scoop WHERE slot BETWEEN :slotFrom and :slotTo GROUP BY period ORDER BY period DESC")
    List<ScooperPeriodStats> getScooperPeriodStats(String scooperPubKeyHash, Long slotFrom, Long slotTo, Long periodLength);

    @Query("SELECT DISTINCT s.scooperPubKeyHash FROM Scoop s ORDER BY s.scooperPubKeyHash")
    List<String> findDistinctScooperPubKeyHashes();

    /**
     * Find the most recent scoop for a given scooper.
     *
     * @param scooperPubKeyHash The scooper's public key hash
     * @return The most recent scoop, if any exists
     */
    Optional<Scoop> findFirstByScooperPubKeyHashOrderBySlotDesc(String scooperPubKeyHash);

    /**
     * Get protocol-level time-series stats using Postgres date_trunc for grouping.
     *
     * @param dateStart Start of time range (inclusive)
     * @param dateEnd End of time range (exclusive)
     * @param truncInterval Postgres date_trunc interval: 'day', 'week', 'month', 'hour'
     * @return List of time-series stats, one per period, ordered by period ASC
     */
    @Query(nativeQuery = true, value =
        "SELECT " +
        "  date_trunc(:truncInterval, timestamp) AS period, " +
        "  COUNT(1) AS total_scoops, " +
        "  COALESCE(SUM(orders), 0) AS total_orders, " +
        "  COALESCE(SUM(protocol_fee), 0) AS total_protocol_fee, " +
        "  COALESCE(SUM(transaction_fee), 0) AS total_transaction_fee " +
        "FROM scoops " +
        "WHERE timestamp >= :dateStart AND timestamp < :dateEnd " +
        "GROUP BY 1 " +
        "ORDER BY 1 ASC")
    List<ProtocolTimeSeriesStats> findProtocolTimeSeriesStats(
        LocalDateTime dateStart,
        LocalDateTime dateEnd,
        String truncInterval);

    /**
     * Get protocol-level stats grouped by epoch.
     * Uses existing epoch field instead of date_trunc.
     *
     * @param dateStart Start of time range (inclusive)
     * @param dateEnd End of time range (exclusive)
     * @return List of stats per epoch, with MIN(timestamp) as period marker
     */
    @Query(nativeQuery = true, value =
        "SELECT " +
        "  MIN(timestamp) AS period, " +
        "  COUNT(1) AS total_scoops, " +
        "  COALESCE(SUM(orders), 0) AS total_orders, " +
        "  COALESCE(SUM(protocol_fee), 0) AS total_protocol_fee, " +
        "  COALESCE(SUM(transaction_fee), 0) AS total_transaction_fee " +
        "FROM scoops " +
        "WHERE timestamp >= :dateStart AND timestamp < :dateEnd " +
        "GROUP BY epoch " +
        "ORDER BY epoch ASC")
    List<ProtocolTimeSeriesStats> findProtocolEpochStats(
        LocalDateTime dateStart,
        LocalDateTime dateEnd);

}
