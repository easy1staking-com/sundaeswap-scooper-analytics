CREATE TABLE scoops (
    tx_hash TEXT PRIMARY KEY,
    scooper_pub_key_hash TEXT NOT NULL,
    orders BIGINT NOT NULL,
    protocol_fee BIGINT NOT NULL,
    transaction_fee BIGINT NOT NULL,
    slot BIGINT NOT NULL,
    epoch BIGINT NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS scoops_scooper_pub_key_hash_epoch_version ON scoops(scooper_pub_key_hash, epoch, version);
CREATE INDEX IF NOT EXISTS scoops_slot_scooper_pub_key_hash ON scoops(slot DESC, scooper_pub_key_hash);
