package com.easystaking.sundaeswap.scooper.analytics.controller;

import com.easystaking.sundaeswap.scooper.analytics.model.ScooperStats;
import com.easystaking.sundaeswap.scooper.analytics.repository.ScoopRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scoopers")
@AllArgsConstructor
@Slf4j
public class ScooperController {

    private final ScoopRepository scoopRepository;

    @GetMapping("/stats")
    public ResponseEntity<List<ScooperStats>> getStats() {
        List<ScooperStats> scooperStats = scoopRepository
                .findScooperStats();
                scooperStats.forEach(foo -> log.info("{}, {}, {}", foo.getPubKeyHash(), foo.getTotalScoops(), foo.getTotalOrders()));
        return ResponseEntity.ok(scooperStats);
    }

}
