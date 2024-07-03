package com.easystaking.sundaeswap.scooper.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@IdClass(UtxoId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private String txHash;

    @Id
    private Integer txIndex;

    private Long slot;

    private String txCbor;

    private Boolean isSpent;

    private LocalDateTime utxoTimestamp;

}
