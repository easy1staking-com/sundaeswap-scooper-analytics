package com.easystaking.sundaeswap.scooper.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ScooperService {

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

}
