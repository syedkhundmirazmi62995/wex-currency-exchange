package com.wex.exchange.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI currencyExchangeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("WEX Currency Exchange API")
                .version("v1")
                .description("Stores USD purchases and retrieves them converted into other currencies "
                        + "using the U.S. Treasury Reporting Rates of Exchange."));
    }
}
