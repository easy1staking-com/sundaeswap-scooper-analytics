package com.easystaking.sundaeswap.scooper.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.easystaking.sundaeswap.scooper.analytics")
@EnableJpaRepositories("com.easystaking.sundaeswap.scooper.analytics.repository")
@EntityScan("com.easystaking.sundaeswap.scooper.analytics.entity")

public class SundaeswapScooperAnalyticsApp {

    public static void main(String[] args) {
        SpringApplication.run(SundaeswapScooperAnalyticsApp.class, args);
    }


}
