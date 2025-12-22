package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import com.easystaking.sundaeswap.scooper.analytics.model.Constants;
import com.easystaking.sundaeswap.scooper.analytics.model.contract.ProtocolSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SettingsService {

    private final UtxoRepository utxoRepository;

    private final SettingsParser settingsParser;

    public Optional<ProtocolSettings> getProtocolSettings() {
        log.info("Getting protocol settings");
        return utxoRepository.findUnspentByOwnerPaymentCredential(Constants.SETTINGS_NFT_POLICY_ID, Pageable.unpaged())
                .map(foo -> {
                    log.info("foo.sieze: {}", foo.size());
                    return foo;
                })
                .stream()
                .flatMap(Collection::stream)
                .filter(utxoEntity -> {
                    log.info("Getting protocol settings for utxo entity: {}", utxoEntity);
                    return utxoEntity.getAmounts().stream().anyMatch(amount -> Constants.SETTINGS_NFT_POLICY_ID.equals(amount.getPolicyId()));
                })
                .flatMap(utxoEntity -> {
                    var fooOpt = settingsParser.parse(utxoEntity.getInlineDatum()).stream();
                    log.info("Getting protocol settings for foo opt: {}", fooOpt);
                    return fooOpt;
                })
                .findFirst();
    }


}
