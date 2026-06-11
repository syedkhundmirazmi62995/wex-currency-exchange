package com.wex.exchange.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * The RestClient used to talk to the Treasury. Connect and read timeouts are deliberately short:
     * the rates endpoint is fast in practice, and a slow or hung upstream shouldn't tie up a request
     * thread waiting on it.
     */
    @Bean
    RestClient treasuryRestClient(RestClient.Builder builder) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(5));
        return builder
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
