package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import com.easystaking.sundaeswap.scooper.analytics.model.Constants;
import com.easystaking.sundaeswap.scooper.analytics.model.contract.ProtocolSettings;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Service
public class ScooperHashService {

    private final UtxoRepository utxoRepository;

    private final SettingsParser settingsParser;

    private final Set<String> scooperPkhs = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        utxoRepository.findUnspentByOwnerPaymentCredential(Constants.SETTINGS_NFT_POLICY_ID, Pageable.unpaged())
                .stream()
                .flatMap(Collection::stream)
                .filter(foo -> foo.getAmounts().stream().anyMatch(amount -> Constants.SETTINGS_NFT_POLICY_ID.equals(amount.getPolicyId())))
                .flatMap(foo -> settingsParser.parse(foo.getInlineDatum()).stream())
                .forEach(this::updateScooperPkhs);
    }

    @EventListener
    public void processNewSettings(AddressUtxoEvent addressUtxoEvent) {
        addressUtxoEvent.getTxInputOutputs()
                .stream()
                .flatMap(outputs -> outputs.getOutputs().stream())
                .filter(addressUtxo -> Constants.SETTINGS_NFT_POLICY_ID.equals(addressUtxo.getOwnerPaymentCredential()))
                .flatMap(addressUtxo -> settingsParser.parse(addressUtxo.getInlineDatum()).stream())
                .forEach(this::updateScooperPkhs);
    }

    private void updateScooperPkhs(ProtocolSettings protocolSettings) {
        scooperPkhs.addAll(protocolSettings.scooperPubKeyHashes());
        scooperPkhs.removeIf(hash -> !protocolSettings.scooperPubKeyHashes().contains(hash));
    }


    public boolean isScooperPubKeyHash(String hash) {
        return scooperPkhs.contains(hash);
    }

}
