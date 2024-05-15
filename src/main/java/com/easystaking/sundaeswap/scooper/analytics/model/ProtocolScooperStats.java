package com.easystaking.sundaeswap.scooper.analytics.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProtocolScooperStats(Long totalScoops,
                                   Long totalOrders,
                                   Long totalUserFee,
                                   Long totalTransactionFee,
                                   List<ExtendedScooperStats> scooperStats) {

}
