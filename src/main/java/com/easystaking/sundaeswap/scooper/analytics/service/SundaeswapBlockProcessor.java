package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.common.NetworkType;
import com.bloxbean.cardano.yaci.core.config.YaciConfig;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.handshake.util.N2NVersionTableConstant;
import com.bloxbean.cardano.yaci.helper.reactive.BlockStreamer;
import com.easystaking.sundaeswap.scooper.analytics.config.AppConfig;
import com.easystaking.sundaeswap.scooper.analytics.entity.Order;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.repository.OrderRepository;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.conversions.CardanoConverters;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.bloxbean.cardano.yaci.core.model.RedeemerTag.Spend;

@Component
@RequiredArgsConstructor
@Slf4j
public class SundaeswapBlockProcessor {

    private static final String SUNDAE_ESCROW_ADDRESS = "addr1w8ax5k9mutg07p2ngscu3chsauktmstq92z9de938j8nqacprc9mw";

    private static final String SUNDAE_POOL_ADDRESS = "addr1x8srqftqemf0mjlukfszd97ljuxdp44r372txfcr75wrz26rnxqnmtv3hdu2t6chcfhl2zzjh36a87nmd6dwsu3jenqsslnz7e";

    private static final Point INITIAL_POINT = new Point(123706722, "09fde708bac045ac54d1f9e2f215e6c235518d1c43b39b645d9fc46d51c76321");
//    private static final Point INITIAL_POINT = new Point(123714534L, "b343c850a06b5782f530fcb5f740ba0c9368bc9d168debfc22eff17742810293");

    private static final Long SCOOP_BASE_FEE = 332000L;

    private static final Long SCOOP_INCREMENTAL_FEE = 168000L;

    private final ScooperService scooperService;

    private final AddressService addressService;

    private final ScoopRepository scoopRepository;

    private final OrderRepository orderRepository;

    private final BFBackendService bfBackendService;

    private final CardanoConverters cardanoConverters;

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
                    N2NVersionTableConstant.v4AndAbove(Networks.mainnet().getProtocolMagic()));
        } else {
            log.info("INIT - no custom node configure, connecting to IOG's relay");
            streamer = BlockStreamer.fromPoint(NetworkType.MAINNET, point);
        }
        return streamer;
    }

    @PostConstruct
    public void start() throws Exception {

        YaciConfig.INSTANCE.setReturnTxBodyCbor(true);

        var escrowAddress = new Address(SUNDAE_ESCROW_ADDRESS);
        Optional<byte[]> escrowPkhOpt = escrowAddress.getPaymentCredentialHash();
        if (escrowPkhOpt.isEmpty()) {
            throw new Exception("could not derive escrow address");
        }
        var escrowPkh = HexUtil.encodeHexString(escrowPkhOpt.get());
        log.info("escrowPkh:{} ", escrowPkh);

        List<String> allowedScooperPubKeyHashes = scooperService.getAllowedScooperPubKeyHashes();

        getBlockStreamer()
                .stream()
                .subscribe(block -> {
                    var slot = block.getHeader().getHeaderBody().getSlot();
                    var epoch = cardanoConverters.slot().slotToEpoch(slot);

                    if (block.getHeader().getHeaderBody().getBlockNumber() % 10 == 0) {
                        log.info("Processing block epoch/number: {}/{}", epoch, block.getHeader().getHeaderBody().getBlockNumber());
                    }


                    for (int i = 0; i < block.getTransactionBodies().size(); i++) {
                        TransactionBody transactionBody = block.getTransactionBodies().get(i);

                        // Look for Orders
                        try {
                            for (int j = 0; j < transactionBody.getOutputs().size(); j++) {
                                TransactionOutput transactionOutput = transactionBody.getOutputs().get(j);

                                if (addressService.extractShelleyAddress(transactionOutput.getAddress())
                                        .flatMap(Address::getPaymentCredentialHash)
                                        .map(HexUtil::encodeHexString)
                                        .stream().anyMatch(addressPkh -> addressPkh.equals(escrowPkh))) {

                                    var timestamp = cardanoConverters.slot().slotToTime(slot);

                                    var order = Order.builder()
                                            .txHash(transactionBody.getTxHash())
                                            .txIndex(j)
                                            .slot(slot)
                                            .txCbor(transactionBody.getCbor())
                                            .utxoTimestamp(timestamp)
                                            .isSpent(false)
                                            .build();

                                    orderRepository.save(order);

                                }

                            }
                        } catch (Exception e) {
                            log.warn("error", e);
                        }


                        // Check if it's a scoop
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
