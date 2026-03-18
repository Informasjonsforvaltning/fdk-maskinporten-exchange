package no.digdir.fdk.maskinportenexchange.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "security.api-key")
data class ApiKeyProperties(
    @DefaultValue("X-API-Key")
    val headerName: String,
    val value: String?
)
