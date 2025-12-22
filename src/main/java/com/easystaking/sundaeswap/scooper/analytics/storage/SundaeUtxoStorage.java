package com.easystaking.sundaeswap.scooper.analytics.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import com.easystaking.sundaeswap.scooper.analytics.model.Constants;
import com.easystaking.sundaeswap.scooper.analytics.service.ScooperHashService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Repository
@Slf4j
public class SundaeUtxoStorage extends UtxoStorageImpl {

    private final ScooperHashService scooperHashService;

    public SundaeUtxoStorage(UtxoRepository utxoRepository,
                             TxInputRepository spentOutputRepository,
                             DSLContext dsl,
                             UtxoCache utxoCache,
                             PlatformTransactionManager transactionManager,
                             ScooperHashService scooperHashService) {
        super(utxoRepository, spentOutputRepository, dsl, utxoCache, transactionManager);
        this.scooperHashService = scooperHashService;
    }

    private boolean isRelevantPubKeyHash(String hash) {
        return hash != null && (Constants.SETTINGS_NFT_POLICY_ID.equals(hash) ||
                scooperHashService.isScooperPubKeyHash(hash));
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        var utxoToSave = addressUtxoList.stream()
                .filter(addressUtxo -> isRelevantPubKeyHash(addressUtxo.getOwnerPaymentCredential()))
                .toList();
        super.saveUnspent(utxoToSave);
    }

}
