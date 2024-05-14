package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.common.NetworkType;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.handshake.util.N2NVersionTableConstant;
import com.bloxbean.cardano.yaci.helper.reactive.BlockStreamer;
import com.easystaking.sundaeswap.scooper.analytics.config.AppConfig;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class SundaeswapBlockProcessor {

    private static final String SUNDAE_POOL_ADDRESS = "addr1x8srqftqemf0mjlukfszd97ljuxdp44r372txfcr75wrz26rnxqnmtv3hdu2t6chcfhl2zzjh36a87nmd6dwsu3jenqsslnz7e";

    private static final Point INITIAL_POINT = new Point(123724823, "624435475545b670ddb3dc14ac64a3fdff9454601cb9362f676ddd32dde7a5ef");

    private final ScooperService scooperService;

    private final ScoopRepository scoopRepository;

    private final BFBackendService bfBackendService;

    private final AppConfig.BlockStreamerConfig blockStreamerConfig;

    @PostConstruct
    public void start() {

        Optional<Scoop> lastPersistedScoop = scoopRepository.findAllByOrderBySlotDesc(Limit.of(1));

        Point point;
        if (lastPersistedScoop.isEmpty()) {
            log.info("INIT - no tx found in db, syncing from point: {}.{}", INITIAL_POINT.getSlot(), INITIAL_POINT.getHash());
            point = INITIAL_POINT;
        } else {
            log.info("INIT - last tx: {}", lastPersistedScoop.get().getTxHash());
            var hash = lastPersistedScoop.get().getTxHash();
            try {
                var response = bfBackendService.getTransactionService().getTransaction(hash);
                if (response.isSuccessful()) {
                    var tx = response.getValue();
                    log.info("INIT - Tx: {}, Block hash:{}, block height: {}", hash, tx.getBlock(), tx.getSlot());
                    point = new Point(tx.getSlot(), tx.getBlock());
                } else {
                    log.info("INIT - error while retrieving most recent tx details, syncing from point: {}.{}", INITIAL_POINT.getSlot(), INITIAL_POINT.getHash());
                    point = INITIAL_POINT;
                }

            } catch (Exception e) {
                log.warn("could not fetch transaction and its block", e);
                point = INITIAL_POINT;
            }
        }

        List<String> allowedScooperPubKeyHashes = scooperService.getAllowedScooperPubKeyHashes();

        var scoops = new HashMap<String, AtomicLong>();
        var scooperFees = new HashMap<String, AtomicLong>();

        BlockStreamer streamer;
        if (blockStreamerConfig.getBlockStreamerHost() != null && blockStreamerConfig.getBlockStreamerPort() != null) {
            log.info("INIT - connecting to node, host: {}, port: {}", blockStreamerConfig.getBlockStreamerHost(), blockStreamerConfig.getBlockStreamerPort());
            streamer = BlockStreamer.fromPoint(blockStreamerConfig.getBlockStreamerHost(),
                    blockStreamerConfig.getBlockStreamerPort(),
                    point,
                    N2NVersionTableConstant.v11AndAbove(Networks.mainnet().getProtocolMagic()));
        } else {
            log.info("INIT - no custom node configure, connecting to IOG's relay");
            streamer = BlockStreamer.fromPoint(NetworkType.MAINNET, point);
        }

        log.info("about to start streaming");

        streamer.stream()
                .subscribe(block -> {

                    try {

                        if (block.getHeader().getHeaderBody().getBlockNumber() % 10 == 0) {
                            log.info("Processing block number: {}", block.getHeader().getHeaderBody().getBlockNumber());
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

                                    var potentialSwaps = witnesses.getRedeemers().size() - 2;
                                    var numSwaps = Math.max(0, potentialSwaps);

                                    requiredSigners.forEach(signer -> {

                                        if (allowedScooperPubKeyHashes.contains(signer)) {

                                            Scoop dbScoop = Scoop.builder()
                                                    .txHash(transactionBody.getTxHash())
                                                    .scooperPubKeyHash(signer)
                                                    .orders((long) potentialSwaps)
                                                    .fees(transactionBody.getFee().longValue())
                                                    .epoch(0L)
                                                    .slot(block.getHeader().getHeaderBody().getSlot())
                                                    .version(3L)
                                                    .build();

                                            scoopRepository.save(dbScoop);

                                            var scoop = scoops.get(signer);
                                            if (scoop == null) {
                                                scoops.put(signer, new AtomicLong(numSwaps));
                                            } else {
                                                scoop.addAndGet(numSwaps);
                                            }

                                            var fees = scooperFees.get(signer);
                                            if (fees == null) {
                                                scooperFees.put(signer, new AtomicLong(transactionBody.getFee().longValue()));
                                            } else {
                                                fees.addAndGet(transactionBody.getFee().longValue());
                                            }

                                        }


                                    });

                                }

                            }

                        }

                    } catch (Exception e) {
                        log.warn("error", e);
                    }


                });


    }

}
