package com.easystaking.sundaeswap.scooper.analytics.service;

import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.handshake.util.N2NVersionTableConstant;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.easystaking.sundaeswap.scooper.analytics.config.AppConfig;
import com.easystaking.sundaeswap.scooper.analytics.entity.Scoop;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.bloxbean.cardano.yaci.core.common.Constants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SundaeswapBlockProcessor {

    private static final Point INITIAL_POINT = new Point(123706722, "09fde708bac045ac54d1f9e2f215e6c235518d1c43b39b645d9fc46d51c76321");
//    private static final Point INITIAL_POINT = new Point(123714534L, "b343c850a06b5782f530fcb5f740ba0c9368bc9d168debfc22eff17742810293");

    private final DefaultBlockchainDataListener defaultBlockchainDataListener;

    private final ScoopRepository scoopRepository;

    private final BFBackendService bfBackendService;

    private final AppConfig.CardanoNodeConfig cardanoNodeConfig;

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

    private BlockSync blockSync() {
        BlockSync streamer;
        if (cardanoNodeConfig.getCardanoNodeHost() != null && cardanoNodeConfig.getCardanoNodePort() != null) {
            log.info("INIT - connecting to node, host: {}, port: {}", cardanoNodeConfig.getCardanoNodeHost(), cardanoNodeConfig.getCardanoNodePort());
            streamer = new BlockSync(cardanoNodeConfig.getCardanoNodeHost(),
                    cardanoNodeConfig.getCardanoNodePort(),
                    WELL_KNOWN_MAINNET_POINT,
                    N2NVersionTableConstant.v4AndAbove(Networks.mainnet().getProtocolMagic()));
        } else {
            log.info("INIT - no custom node configure, connecting to IOG's relay");
            streamer = new BlockSync(MAINNET_IOHK_RELAY_ADDR, MAINNET_IOHK_RELAY_PORT, WELL_KNOWN_MAINNET_POINT, N2NVersionTableConstant.v4AndAbove(Networks.mainnet().getProtocolMagic()));
        }
        return streamer;
    }

    @PostConstruct
    public void start() throws Exception {

        var point = getInitialPoint();

        blockSync().startSync(point, defaultBlockchainDataListener);

    }

}
