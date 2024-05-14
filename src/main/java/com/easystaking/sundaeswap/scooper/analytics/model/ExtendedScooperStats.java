package com.easystaking.sundaeswap.scooper.analytics.model;

import java.util.function.Function;

public record ExtendedScooperStats(String pubKeyHash,
                                   String address,
                                   Long totalScoops,
                                   Long totalOrders,
                                   Long totalFees) {

    public static ExtendedScooperStats from(ScooperStats scooperStats, Function<String, String> toAddress) {
        return new ExtendedScooperStats(scooperStats.getPubKeyHash(),
                toAddress.apply(scooperStats.getPubKeyHash()),
                scooperStats.getTotalScoops(),
                scooperStats.getTotalOrders(),
                scooperStats.getTotalFees());
    }

}
