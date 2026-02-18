package com.example.fdkmaskinportenexchange.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("FDK Maskinporten API")
                    .description("API for obtaining and managing Maskinporten access tokens")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Maskinporten POC")
                    )
            )
    }
}
