package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionInput;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.model.contract.ProtocolSettings;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.conversions.CardanoConverters;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

import static com.bloxbean.cardano.client.address.AddressType.Base;
import static com.bloxbean.cardano.client.address.AddressType.Enterprise;
import static com.bloxbean.cardano.yaci.core.model.RedeemerTag.Spend;
import static com.easystaking.sundaeswap.scooper.analytics.model.Constants.POOL_NFT_POLICY_ID;
import static com.easystaking.sundaeswap.scooper.analytics.model.Constants.SETTINGS_NFT_POLICY_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScooperEventListener {

    private final Map<TransactionInput, Utxo> knownReferenceInputs = new HashMap<>();

    private final BFBackendService bfBackendService;

    private final ScoopRepository scoopRepository;

    private final SettingsParser settingsParser;

    private final CardanoConverters cardanoConverters;

    private final SimpMessagingTemplate simpMessagingTemplate;

    @EventListener
    @Transactional
    public void processEvent(BlockEvent blockEvent) {

        var slot = blockEvent.getMetadata().getSlot();
        var epoch = cardanoConverters.slot().slotToEpoch(slot);

        var block = blockEvent.getBlock();

        var blockTxHashes = block.getTransactionBodies().stream().map(TransactionBody::getTxHash).toList();

        if (block.getHeader().getHeaderBody().getBlockNumber() % 10 == 0) {
            log.info("Processing block slot/epoch/number: {}/{}/{}", slot, epoch, block.getHeader().getHeaderBody().getBlockNumber());
        }

        for (int i = 0; i < block.getTransactionBodies().size(); i++) {
            TransactionBody transactionBody = block.getTransactionBodies().get(i);

            if (transactionBody
                    .getOutputs()
                    .stream()
                    .flatMap(transactionOutput -> Optional.ofNullable(transactionOutput.getAddress()).stream())
                    .filter(address -> address.startsWith("addr1"))
                    .anyMatch(shelleyStringAddress -> {
                        try {
                            var address = new Address(shelleyStringAddress);
                            if (List.of(Base, Enterprise).contains(address.getAddressType())) {
                                return HexUtil.encodeHexString(address.getPaymentCredentialHash().get()).equals(POOL_NFT_POLICY_ID);
                            } else {
                                return false;
                            }
                        } catch (Exception e) {
                            log.warn("Error while processing address: {}", shelleyStringAddress);
                            return false;
                        }
                    })) {
                var protocolSettingsOpt = Optional.ofNullable(transactionBody.getReferenceInputs()).flatMap(this::resolveProtocolSettings);
                if (protocolSettingsOpt.isEmpty()) {
                    log.warn("protocolFees are unexpectedly empty");
                    return;
                }

                var protocolSettings = protocolSettingsOpt.get();
                var protocolFees = protocolSettings.protocolFees();
                var scooperPkhs = protocolSettings.scooperPubKeyHashes();

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

                        if (scooperPkhs.contains(signer)) {

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

    @EventListener
    @Transactional
    public void processRollback(RollbackEvent rollbackEvent) {
        var point = rollbackEvent.getRollbackTo();
        if (point.getSlot() > 0 && point.getHash() != null) {
            var numDeletedScoops = scoopRepository.deleteBySlotGreaterThan(point.getSlot());
            log.info("rollback to slot: {}, numDeletedScoops: {}", point.getSlot(), numDeletedScoops);
        } else {
            log.info("ignoring invalid rollback");
        }
    }

    private Optional<ProtocolSettings> resolveProtocolSettings(Set<TransactionInput> referenceInputs) {
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

    private Optional<ProtocolSettings> resolveProtocolFees(Utxo utxo) {
        return utxo
                .getAmount()
                .stream()
                .filter(amount -> amount.getUnit().startsWith(SETTINGS_NFT_POLICY_ID))
                .findAny()
                .flatMap(amount -> settingsParser.parse(utxo.getInlineDatum()));
    }

}
