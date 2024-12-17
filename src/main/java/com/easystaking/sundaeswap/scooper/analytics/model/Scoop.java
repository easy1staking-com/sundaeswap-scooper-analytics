package com.easystaking.sundaeswap.scooper.analytics.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Scoop(Long timestamp, String txHash, Long numOrders, String scooperHash, Boolean isMempool) {

}
