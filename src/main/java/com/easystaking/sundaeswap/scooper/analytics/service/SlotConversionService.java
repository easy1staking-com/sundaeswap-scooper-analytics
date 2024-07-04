package com.easystaking.sundaeswap.scooper.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.conversions.CardanoConverters;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotConversionService {

    private final CardanoConverters cardanoConverters;

    public long toSlot(LocalDateTime utcTime) {
        return cardanoConverters.time().toSlot(utcTime);
    }

    public long toSlotFromNow(Function<LocalDateTime, LocalDateTime> f) {
        var from = f.apply(LocalDateTime.now(ZoneOffset.UTC));
        log.info("from: {}", from);
        return cardanoConverters.time().toSlot(from);
    }


}
