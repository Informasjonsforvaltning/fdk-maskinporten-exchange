package no.digdir.fdk.maskinportenexchange.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Value("\${security.api-key.header-name:X-API-Key}")
    private lateinit var apiKeyHeaderName: String

    @Bean
    fun openAPI(): OpenAPI {
        val schemeName = "ApiKeyAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("FDK Maskinporten API")
                    .description(
                        "API for obtaining and managing Maskinporten access tokens. Use **Authorize** to set the API key required for protected endpoints.",
                    )
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Maskinporten POC"),
                    ),
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        schemeName,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name(apiKeyHeaderName)
                            .description("API key for service-to-service authentication"),
                    ),
            )
            .addSecurityItem(SecurityRequirement().addList(schemeName))
    }
}
