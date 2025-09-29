package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.easystaking.sundaeswap.scooper.analytics.model.contract.ProtocolFees;
import com.easystaking.sundaeswap.scooper.analytics.model.contract.ProtocolSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsParser {

    private final ObjectMapper objectMapper;

    public Optional<ProtocolSettings> parse(String settingsInlineDatum) {
        try {

            PlutusData swapDatum = PlutusData.deserialize(HexUtil.decodeHexString(settingsInlineDatum));
            var settings = objectMapper.writeValueAsString(swapDatum);

            JsonNode jsonNode = JsonUtil.parseJson(settings);
            var fields = jsonNode.path("fields");

            var scooperPkhs = new ArrayList<String>();
            var scoopersPkhsRoot = fields.get(5).path("fields").get(0).path("list");
            scoopersPkhsRoot.iterator().forEachRemaining(node -> scooperPkhs.add(node.path("bytes").asText()));

            var baseFee = readInt(fields.get(7));
            var simpleFee = readInt(fields.get(8));
            var strategyFee = readInt(fields.get(9));

            return Optional.of(new ProtocolSettings(new ProtocolFees(baseFee, simpleFee, strategyFee), scooperPkhs));

        } catch (Exception e) {
            log.warn("could not parse settings", e);
            return Optional.empty();
        }

    }

    private BigInteger readInt(JsonNode node) {
        return BigInteger.valueOf(node.path("int").asLong());
    }

}
