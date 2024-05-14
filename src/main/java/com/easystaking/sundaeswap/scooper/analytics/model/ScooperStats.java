package com.easystaking.sundaeswap.scooper.analytics.model;

public interface ScooperStats {

    String getPubKeyHash();

    Long getTotalScoops();

    Long getTotalOrders();

    Long getTotalFees();

}
