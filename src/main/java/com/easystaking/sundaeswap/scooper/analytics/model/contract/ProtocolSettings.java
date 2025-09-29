package com.easystaking.sundaeswap.scooper.analytics.model.contract;

import java.util.List;

public record ProtocolSettings(ProtocolFees protocolFees, List<String> scooperPubKeyHashes) {
}
