package com.easystaking.sundaeswap.scooper.analytics.model;

public interface ScooperStats {

    String getPubKeyHash();

    Long getEpoch();

    Long getTotalScoops();

    Long getTotalOrders();

    Long getTotalUserFee();

    Long getTotalTransactionFee();

}
