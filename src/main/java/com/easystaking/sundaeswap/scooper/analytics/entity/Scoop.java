package com.easystaking.sundaeswap.scooper.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scoops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scoop {

    @Id
    private String txHash;

    private String scooperPubKeyHash;

    private Long orders;

    private Long userFee;

    private Long transactionFee;

    private Long slot;

    private Long epoch;

    private Long version;

}
