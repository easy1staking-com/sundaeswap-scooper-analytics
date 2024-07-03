package com.easystaking.sundaeswap.scooper.analytics.repository;

import com.easystaking.sundaeswap.scooper.analytics.entity.Order;
import com.easystaking.sundaeswap.scooper.analytics.entity.UtxoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UtxoId> {

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
