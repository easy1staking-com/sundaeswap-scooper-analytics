package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import com.easy1staking.cardano.util.AmountUtil;
import com.easy1staking.util.Pair;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ScooperBalanceService {

    private final UtxoRepository utxoRepository;

    private final SettingsService settingsService;

    public List<Pair<String, BigInteger>> getScooperBalances() {
        return settingsService.getProtocolSettings()
                .stream()
                .flatMap(protocolSettings -> protocolSettings.scooperPubKeyHashes()
                        .stream()
                        .flatMap(hash -> utxoRepository.findUnspentByOwnerPaymentCredential(hash, Pageable.unpaged())
                                .stream()
                                .flatMap(Collection::stream)
                                .flatMap(entity -> entity.getAmounts().stream())
                                .map(AmountUtil::toValue)
                                .map(Value::getCoin)
                                .reduce(BigInteger::add)
                                .map(adaBalance -> new Pair<>(hash, adaBalance))
                                .stream()))
                .toList();
    }


}
