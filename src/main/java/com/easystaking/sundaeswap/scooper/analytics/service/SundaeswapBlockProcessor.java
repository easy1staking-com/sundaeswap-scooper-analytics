package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.handshake.util.N2NVersionTableConstant;
import com.bloxbean.cardano.yaci.helper.reactive.BlockStreamer;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SundaeswapBlockProcessor {

    private static final String SUNDAE_POOL_ADDRESS = "addr1x8srqftqemf0mjlukfszd97ljuxdp44r372txfcr75wrz26rnxqnmtv3hdu2t6chcfhl2zzjh36a87nmd6dwsu3jenqsslnz7e";

    private final ScooperService scooperService;

    private final ScoopRepository scoopRepository;

    private final BFBackendService bfBackendService;

    @PostConstruct
    public void start() throws ApiException {

        Optional<Scoop> orderBySlotDesc = scoopRepository.findAllByOrderBySlotDesc(Limit.of(1));

        if (orderBySlotDesc.isEmpty()) {
            log.info("EMPTY");
        } else {
            log.info("last tx: {}", orderBySlotDesc.get().getTxHash());

            var hash = orderBySlotDesc.get().getTxHash();
            TransactionContent value = bfBackendService.getTransactionService().getTransaction(hash).getValue();
            log.info("Tx: {}, Block hash:{}, block height: {}", hash, value.getBlock(), value.getSlot());

        }

        List<String> allowedScooperPubKeyHashes = scooperService.getAllowedScooperPubKeyHashes();

        var scoops = new HashMap<String, AtomicLong>();
        var scooperFees = new HashMap<String, AtomicLong>();

        var txCounter = new AtomicLong(0L);

        var point = new Point(123724823, "624435475545b670ddb3dc14ac64a3fdff9454601cb9362f676ddd32dde7a5ef");
//        BlockStreamer streamer = BlockStreamer.fromPoint(NetworkType.MAINNET, point);
        BlockStreamer streamer = BlockStreamer.fromPoint("ryzen",
                30000,
                point,
                N2NVersionTableConstant.v4AndAbove(Networks.mainnet().getProtocolMagic()));

        streamer.stream()
                .subscribe(block -> {

                    log.info("blockHash: {}", block.getHeader().getHeaderBody().getBlockHash());

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

                                txCounter.incrementAndGet();

                            }

                        }

                    }

                    log.info("txCounter: {}", txCounter.get());

                    if (block.getHeader().getHeaderBody().getBlockNumber() % 25 == 0) {
                        var scooperLeader = scoops.entrySet()
                                .stream()
                                .sorted(Comparator.comparingLong(foo -> foo.getValue().longValue()))
                                .collect(Collectors.toList());
                        Collections.reverse(scooperLeader);
                        log.info("\n");
                        log.info("LEADERBOARD");
                        scooperLeader.forEach(entry -> {
                            log.info("{}: {}", entry.getKey(), entry.getValue().get());
                        });
                        log.info("\n");

                        var feesLeader = scooperFees.entrySet()
                                .stream()
                                .sorted(Comparator.comparingLong(foo -> foo.getValue().longValue()))
                                .collect(Collectors.toList());
                        Collections.reverse(feesLeader);


                        log.info("\n");
                        log.info("LEADERBOARD - FEES");
                        feesLeader.forEach(entry -> {
                            log.info("{}: {}", entry.getKey(), entry.getValue().get());
                        });
                        log.info("\n");
                    }

                });


    }

}
