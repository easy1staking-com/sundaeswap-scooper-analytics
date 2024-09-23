package com.easystaking.sundaeswap.scooper.analytics.model.contract;

import java.math.BigInteger;

public record ProtocolFees (BigInteger baseFee, BigInteger simpleFee, BigInteger strategyFee) {
}
