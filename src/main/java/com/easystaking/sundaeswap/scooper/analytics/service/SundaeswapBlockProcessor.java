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

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    private Point getInitialPoint() {
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
        return point;
    }

    private BlockStreamer getBlockStreamer() {
        var point = getInitialPoint();
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
        return streamer;
    }

    @PostConstruct
    public void start() throws Exception {

        List<String> allowedScooperPubKeyHashes = scooperService.getAllowedScooperPubKeyHashes();

        getBlockStreamer()
                .stream()
                .subscribe(block -> {


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

                                var orders = witnesses.getRedeemers().size() - 2;

                                requiredSigners.forEach(signer -> {

                                    if (allowedScooperPubKeyHashes.contains(signer)) {

                                        Scoop dbScoop = Scoop.builder()
                                                .txHash(transactionBody.getTxHash())
                                                .scooperPubKeyHash(signer)
                                                .orders((long) orders)
                                                .fees(transactionBody.getFee().longValue())
                                                .epoch(0L)
                                                .slot(block.getHeader().getHeaderBody().getSlot())
                                                .version(3L)
                                                .build();

                                        scoopRepository.save(dbScoop);

                                    }
                                });

                            }

                        }

                    }


                });


    }

}
