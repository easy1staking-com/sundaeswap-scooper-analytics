package com.easystaking.sundaeswap.scooper.analytics.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Repository
@Slf4j
public class SundaeUtxoStorage extends UtxoStorageImpl {

    public SundaeUtxoStorage(UtxoRepository utxoRepository, TxInputRepository spentOutputRepository, DSLContext dsl, UtxoCache utxoCache, PlatformTransactionManager transactionManager) {
        super(utxoRepository, spentOutputRepository, dsl, utxoCache, transactionManager);
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {

    }

    @Override
    public void saveSpent(List<TxInput> txInputs) {

    }

}
