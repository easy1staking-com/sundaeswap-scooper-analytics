package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.entity.projections.ScooperPeriodStats;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.conversions.CardanoConverters;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.bloxbean.cardano.yaci.core.model.RedeemerTag.Spend;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultBlockchainDataListener implements BlockChainDataListener {

    private static final String SUNDAE_POOL_ADDRESS = "addr1x8srqftqemf0mjlukfszd97ljuxdp44r372txfcr75wrz26rnxqnmtv3hdu2t6chcfhl2zzjh36a87nmd6dwsu3jenqsslnz7e";

    private static final Long SCOOP_BASE_FEE = 332000L;

    private static final Long SCOOP_INCREMENTAL_FEE = 168000L;

    private final ScooperService scooperService;

    private final ScoopRepository scoopRepository;

    private final CardanoConverters cardanoConverters;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private List<String> allowedScooperPubKeyHashes;

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
                    .anyMatch(transactionOutput -> transactionOutput.getAddress().equals(SUNDAE_POOL_ADDRESS))) {

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

                            var protocolFee = SCOOP_BASE_FEE + orders * SCOOP_INCREMENTAL_FEE;

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
                                simpMessagingTemplate.convertAndSend("/topic/messages", dbScoop);
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

}
