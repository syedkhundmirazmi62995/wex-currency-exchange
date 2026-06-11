package com.wex.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /**
     * Use UTC for "now" so the future-date check behaves the same wherever the service runs. Tests
     * swap in a {@link Clock#fixed} to make date logic deterministic.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
