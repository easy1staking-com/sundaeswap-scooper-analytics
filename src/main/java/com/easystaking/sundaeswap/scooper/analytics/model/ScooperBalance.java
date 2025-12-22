package com.easystaking.sundaeswap.scooper.analytics.model;

import com.easy1staking.util.Pair;

import java.math.BigInteger;
import java.util.function.Function;

public record ScooperBalance(String pubKeyHash, String enterpriseAddress, String balance, Long lastScoop) {

    public static ScooperBalance from(Pair<String, BigInteger> balance, Function<String, String> toAddress, Long lastScoop) {
        return new ScooperBalance(balance.first(), toAddress.apply(balance.first()), balance.second().toString(), lastScoop);
    }

}
