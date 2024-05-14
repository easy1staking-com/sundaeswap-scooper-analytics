package com.easystaking.sundaeswap.scooper.analytics.repository;

import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.model.ScooperStats;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoopRepository extends JpaRepository<Scoop, String> {

//        tx_hash TEXT PRIMARY KEY,
//    scooper_pub_key_hash TEXT NOT NULL,
//    orders BIGINT NOT NULL,
//    fees BIGINT NOT NULL,
//    slot BIGINT NOT NULL,
//    epoch BIGINT NOT NULL,
//    version BIGINT NOT NULL


    @Query("SELECT scooperPubKeyHash AS pubKeyHash, count(1) AS totalScoops, sum(orders) AS totalOrders, sum(fees) as totalFees " +
    "FROM Scoop GROUP BY scooperPubKeyHash")
    List<ScooperStats> findScooperStats();

    Optional<Scoop> findAllByOrderBySlotDesc(Limit limit);

    //    @Query(value = "SELECT DISTINCT ON (policy_id, asset_name) * " +
//            "FROM oracle_update ORDER BY policy_id, asset_name, utxo_timestamp DESC", nativeQuery = true)
//    List<OracleUpdate> findLatestPrices();

//    @Modifying
//    @Query("DELETE FROM OracleUpdate WHERE slotCreated > :target")
//    void deleteAllAfterSlot(@Param("target") long target);
//
//
//    @Query(value = "SELECT DISTINCT ON (policy_id, asset_name) * " +
//            "FROM oracle_update ORDER BY policy_id, asset_name, utxo_timestamp DESC", nativeQuery = true)
//    List<OracleUpdate> findLatestPrices();
//
//    @Modifying
//    @Query("UPDATE OracleUpdate SET superseded = true, slotSpent = :slotSpent WHERE txHash = :txHash AND outputIndex = :outputIndex")
//    void setPriceSuperseded(String txHash, Long outputIndex, Long slotSpent);
//
//    @Modifying
//    @Query("UPDATE OracleUpdate SET superseded = false, slotSpent = null WHERE slotSpent > :slotRollback")
//    void restorePriceActive(Long slotRollback);

}
