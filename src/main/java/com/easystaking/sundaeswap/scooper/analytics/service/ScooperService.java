package com.easystaking.sundaeswap.scooper.analytics.service;

import com.easystaking.sundaeswap.scooper.analytics.entity.projections.ScooperPeriodStats;
import com.easystaking.sundaeswap.scooper.analytics.model.PeriodType;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cardanofoundation.conversions.CardanoConverters;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ScooperService {

    private static final Long HOUR_IN_SECONDS = 60L * 60L;

    private static final Long DAYS_IN_HOURS = 24 * HOUR_IN_SECONDS;

    private static final Long WEEKS_IN_HOURS = 7 * DAYS_IN_HOURS;

    private final CardanoConverters converter;

    private final ScoopRepository scoopRepository;

    @Getter
    private final List<String> allowedScooperPubKeyHashes = new ArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() throws Exception {
        try (InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("./scoopers-pkh.json")) {
            var scooperPkh = objectMapper.readValue(resourceAsStream, String[].class);
            allowedScooperPubKeyHashes.addAll(Arrays.asList(scooperPkh));
        }
    }

    public List<ScooperPeriodStats> getScooperStats(String scooperPubKeyHash, Integer periodLength, PeriodType type) {

        return switch (type) {
            case HOURS -> {
                var topOfTheHour = LocalDateTime.now(ZoneOffset.UTC).withMinute(0).withSecond(0);
                var hoursAgo = topOfTheHour.minusHours(periodLength);
                var slotFrom = converter.time().toSlot(hoursAgo);
                var slotTo = converter.time().toSlot(topOfTheHour);
                yield scoopRepository.getScooperPeriodStats(scooperPubKeyHash, slotFrom, slotTo, HOUR_IN_SECONDS);
            }
            case DAYS -> {
                var todayMidnight = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
                var xDaysAgoMidnight = todayMidnight.minusDays(periodLength);
                var slotFrom = converter.time().toSlot(xDaysAgoMidnight);
                var slotTo = converter.time().toSlot(todayMidnight);
                yield scoopRepository.getScooperPeriodStats(scooperPubKeyHash, slotFrom, slotTo, DAYS_IN_HOURS);
            }
        };


    }

}
