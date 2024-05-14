package com.easystaking.sundaeswap.scooper.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
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
        File file = ResourceUtils.getFile("classpath:scoopers-pkh.json");
        var scooperPkh = objectMapper.readValue(file, String[].class);
        allowedScooperPubKeyHashes.addAll(Arrays.asList(scooperPkh));
    }

}
