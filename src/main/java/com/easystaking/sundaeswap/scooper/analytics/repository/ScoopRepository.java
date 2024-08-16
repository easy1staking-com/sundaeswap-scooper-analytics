package com.easystaking.sundaeswap.scooper.analytics.repository;

import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.entity.projections.ScooperPeriodStats;
import com.easystaking.sundaeswap.scooper.analytics.model.ScooperStats;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

}
