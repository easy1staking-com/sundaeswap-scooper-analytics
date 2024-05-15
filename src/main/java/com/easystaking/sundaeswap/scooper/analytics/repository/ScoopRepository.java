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

    List<Scoop> findAllBySlotLessThanOrderBySlotDesc(Long slot, Limit limit);

    @Query("SELECT scooperPubKeyHash AS pubKeyHash, count(1) AS totalScoops, sum(orders) AS totalOrders, " +
            "sum(protocolFee) as totalProtocolFee, sum(transactionFee) as totalTransactionFee " +
            "FROM Scoop GROUP BY scooperPubKeyHash")
    List<ScooperStats> findScooperStats();

    @Query("SELECT scooperPubKeyHash AS pubKeyHash, count(1) AS totalScoops, sum(orders) AS totalOrders, " +
            "sum(protocolFee) as totalProtocolFee, sum(transactionFee) as totalTransactionFee " +
            "FROM Scoop " +
            "WHERE epoch = :epoch " +
            "GROUP BY scooperPubKeyHash, epoch ")
    List<ScooperStats> findScooperStatsByEpoch(Long epoch);

    Optional<Scoop> findAllByOrderBySlotDesc(Limit limit);

}
