package com.easystaking.sundaeswap.scooper.analytics.entity;

import lombok.Data;

@Data
public class UtxoId {

    private String txHash;

    private Integer txIndex;

}
