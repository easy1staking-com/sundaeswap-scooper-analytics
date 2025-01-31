package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.model.contract.ProtocolFees;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.conversions.CardanoConverters;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

import static com.bloxbean.cardano.yaci.core.model.RedeemerTag.Spend;
import static com.easystaking.sundaeswap.scooper.analytics.model.Constants.POOL_ADDRESS_V2;
import static com.easystaking.sundaeswap.scooper.analytics.model.Constants.SETTINGS_NFT_POLICY_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultBlockchainDataListener implements BlockChainDataListener {

    private final Map<TransactionInput, Utxo> knownReferenceInputs = new HashMap<>();

    private final BFBackendService bfBackendService;

    private final ScooperService scooperService;

    private final ScoopRepository scoopRepository;

    private final SettingsParser settingsParser;

    private final CardanoConverters cardanoConverters;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private List<String> allowedScooperPubKeyHashes;

    @org.springframework.context.event.EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        if (rollbackEvent.getRollbackTo() != null && rollbackEvent.getRollbackTo().getSlot() >= 0) {
            log.info("processing valid rollback: {}", rollbackEvent);
            var slot = rollbackEvent.getRollbackTo().getSlot();
            var numDeletedScoops = scoopRepository.deleteBySlotGreaterThan(slot);
            log.info("rollback to slot: {}, numDeletedScoops: {}", slot, numDeletedScoops);
        } else {
            log.info("detected invalid rollback: {}", rollbackEvent);
        }
    }

    @EventListener
    @Transactional
    public void eventListener(BlockEvent blockEvent) {
    }

    @Transactional
    @Override
    public void onBlock(Era era, Block block, List<Transaction> transactions) {

        var slot = block.getHeader().getHeaderBody().getSlot();
        var epoch = cardanoConverters.slot().slotToEpoch(slot);

        var blockTxHashes = block.getTransactionBodies().stream().map(TransactionBody::getTxHash).toList();

        if (block.getHeader().getHeaderBody().getBlockNumber() % 10 == 0) {
            log.info("Processing block slot/epoch/number: {}/{}/{}", slot, epoch, block.getHeader().getHeaderBody().getBlockNumber());
        }

        for (int i = 0; i < block.getTransactionBodies().size(); i++) {
            TransactionBody transactionBody = block.getTransactionBodies().get(i);

            if (transactionBody
                    .getOutputs()
                    .stream()
                    .anyMatch(transactionOutput -> transactionOutput.getAddress().equals(POOL_ADDRESS_V2))) {

                var protocolFeesOpt = resolveProtocolFees(transactionBody.getReferenceInputs());
                if (protocolFeesOpt.isEmpty()) {
                    log.warn("protocolFees are unexpectedly empty");
                    return;
                }

                var protocolFees = protocolFeesOpt.get();

                Set<String> requiredSigners = transactionBody.getRequiredSigners();
                Witnesses witnesses = block.getTransactionWitness().get(i);
                if (witnesses != null && requiredSigners != null && !requiredSigners.isEmpty()) {


                    var orders = witnesses.getRedeemers()
                            .stream()
                            .filter(redeemer -> redeemer.getTag().equals(Spend))
                            .count() - 1;

                    if (orders <= 0) {
                        log.warn("Unexpected number of orders ({}) for tx: {}", orders, transactionBody.getTxHash());
                    }

                    requiredSigners.forEach(signer -> {

                        if (allowedScooperPubKeyHashes.contains(signer)) {

                            // This is a scoop
                            var numMempoolOrders = transactionBody
                                    .getInputs()
                                    .stream()
                                    .filter(transactionInput -> blockTxHashes.contains(transactionInput.getTransactionId()))
                                    .count();

                            var protocolFee = protocolFees.baseFee().longValue() + orders * protocolFees.simpleFee().longValue();

                            Scoop dbScoop = Scoop.builder()
                                    .txHash(transactionBody.getTxHash())
                                    .scooperPubKeyHash(signer)
                                    .orders(orders)
                                    .protocolFee(protocolFee)
                                    .transactionFee(transactionBody.getFee().longValue())
                                    .epoch(epoch)
                                    .slot(block.getHeader().getHeaderBody().getSlot())
                                    .version(3L)
                                    .numMempoolOrders(numMempoolOrders)
                                    .build();

                            scoopRepository.save(dbScoop);


                            try {
                                var timestamp = cardanoConverters.slot().slotToTime(block.getHeader().getHeaderBody().getSlot()).toEpochSecond(ZoneOffset.UTC) * 1_000;
                                var scoop = new com.easystaking.sundaeswap.scooper.analytics.model.Scoop(timestamp,
                                        dbScoop.getTxHash(),
                                        dbScoop.getOrders(),
                                        dbScoop.getScooperPubKeyHash(),
                                        numMempoolOrders > 0L);

                                simpMessagingTemplate.convertAndSend("/topic/messages", scoop);
                            } catch (Exception e) {
                                log.warn("Error", e);
                            }

                        }
                    });

                }

            }

        }

    }

    @Transactional
    @Override
    public void onRollback(Point point) {
        if (point.getSlot() > 0 && point.getHash() != null) {
            var numDeletedScoops = scoopRepository.deleteBySlotGreaterThan(point.getSlot());
            log.info("rollback to slot: {}, numDeletedScoops: {}", point.getSlot(), numDeletedScoops);
        } else {
            log.info("ignoring invalid rollback");
        }
    }

    @PostConstruct
    public void init() {
        allowedScooperPubKeyHashes = scooperService.getAllowedScooperPubKeyHashes();
    }

    private Optional<ProtocolFees> resolveProtocolFees(Set<TransactionInput> referenceInputs) {
        return referenceInputs
                .stream()
                .flatMap(refInput -> {
                    var utxoOpt = Optional.ofNullable(knownReferenceInputs.get(refInput));
                    if (utxoOpt.isEmpty()) {
                        try {
                            var result = bfBackendService.getUtxoService().getTxOutput(refInput.getTransactionId(), refInput.getIndex());
                            if (result.isSuccessful()) {
                                var utxo = result.getValue();
                                knownReferenceInputs.put(refInput, utxo);
                                return resolveProtocolFees(utxo).stream();
                            }
                        } catch (Exception e) {
                            log.warn("could not resolve ref input", e);
                        }
                    } else {
                        return resolveProtocolFees(utxoOpt.get()).stream();
                    }
                    return Stream.empty();
                })
                .findAny();
    }

    private Optional<ProtocolFees> resolveProtocolFees(Utxo utxo) {
        return utxo
                .getAmount()
                .stream()
                .filter(amount -> amount.getUnit().startsWith(SETTINGS_NFT_POLICY_ID))
                .findAny()
                .flatMap(amount -> settingsParser.parseFees(utxo.getInlineDatum()));
    }

}
