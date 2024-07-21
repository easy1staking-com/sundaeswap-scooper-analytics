package com.easystaking.sundaeswap.scooper.analytics.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.function.Function;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExtendedScooperStats(String pubKeyHash,
                                   String address,
                                   Long totalScoops,
                                   Long totalOrders,
                                   Long totalProtocolFee,
                                   Long totalTransactionFee,
                                   Long totalNumMempoolOrders) {

    public static ExtendedScooperStats from(ScooperStats scooperStats, Function<String, String> toAddress) {
        return new ExtendedScooperStats(scooperStats.getPubKeyHash(),
                toAddress.apply(scooperStats.getPubKeyHash()),
                scooperStats.getTotalScoops(),
                scooperStats.getTotalOrders(),
                scooperStats.getTotalProtocolFee(),
                scooperStats.getTotalTransactionFee(),
                scooperStats.getTotalNumMempoolOrders());
    }

}
