package no.digdir.fdk.maskinportenexchange.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.media.Schema
import no.digdir.fdk.maskinportenexchange.config.MaskinportenProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Component
class MaskinportenClient(
    private val restTemplate: RestTemplate,
    private val properties: MaskinportenProperties,
    private val jwtAssertionBuilder: JwtAssertionBuilder,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(MaskinportenClient::class.java)

    @Retryable(
        retryFor = [ResourceAccessException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0),
    )
    @Throws(Exception::class)
    fun requestToken(scope: String? = null): TokenResponse {
        val assertion = jwtAssertionBuilder.buildJwtAssertion(scope)

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            add("assertion", assertion)
        }

        val request = HttpEntity(body, headers)
        val endpoint = properties.tokenEndpoint
            ?: throw IllegalArgumentException("Token endpoint not configured")

        return try {
            val response = restTemplate.postForEntity(endpoint, request, TokenResponse::class.java)
            val tokenResponse = response.body
                ?: throw IllegalStateException("Empty response from Maskinporten")

            if (tokenResponse.accessToken == null) {
                throw IllegalStateException("Missing access_token in response")
            }

            tokenResponse.scope?.let {
                tokenResponse.scopes = it.split(" ").filter { scope -> scope.isNotBlank() }
            }

            tokenResponse
        } catch (e: HttpClientErrorException) {
            val errorMessage = parseErrorResponse(e.responseBodyAsString, e.statusCode, scope)
            logger.error(
                "HTTP error from Maskinporten: Status={}, Response={}, Error={}",
                e.statusCode,
                e.responseBodyAsString,
                errorMessage,
            )
            throw RuntimeException(errorMessage, e)
        } catch (e: ResourceAccessException) {
            logger.warn("I/O error calling Maskinporten (will retry if attempts remain): {}", e.message)
            throw e
        } catch (e: RestClientException) {
            logger.error("Error calling Maskinporten", e)
            throw RuntimeException("Failed to communicate with Maskinporten", e)
        }
    }

    @Schema(description = "Token response from Maskinporten")
    class TokenResponse {
        @JsonProperty("access_token")
        @Schema(description = "The access token (JWT)", example = "IxC0B76vlWl3fiQhAwZUmD0hr_PPwC9hSIXRdoUslPU=")
        var accessToken: String? = null

        @JsonProperty("token_type")
        @Schema(description = "The type of token", example = "Bearer")
        var tokenType: String? = null

        @JsonProperty("expires_in")
        @Schema(description = "Token expiration time in seconds", example = "599")
        var expiresIn: Int = 0

        @JsonProperty("scope")
        @Schema(description = "Space-separated string of granted scopes", example = "difitest:test1")
        var scope: String? = null

        @Schema(description = "List of individual scopes (computed from scope field)", example = "[\"difitest:test1\"]", hidden = true)
        var scopes: List<String> = emptyList()
    }

    private fun parseErrorResponse(responseBody: String?, statusCode: HttpStatusCode, requestedScope: String?): String {
        val statusValue = statusCode.value()
        val scopeHint = if (requestedScope != null) {
            "The requested scope '$requestedScope' may not be available or you may not have access to it."
        } else {
            ""
        }
        if (responseBody.isNullOrBlank()) {
            return when (statusValue) {
                400 -> "Bad request to Maskinporten. $scopeHint"
                401 -> "Unauthorized. Check your client credentials and key configuration."
                403 -> "Forbidden. You may not have permission to access this resource."
                else -> "Maskinporten returned error: $statusCode"
            }
        }

        return try {
            val errorResponse = objectMapper.readValue(responseBody, ErrorResponse::class.java)
            val errorCode = errorResponse.error ?: "unknown_error"
            val errorDescription = errorResponse.errorDescription ?: ""

            when (errorCode) {
                "invalid_scope" -> {
                    val scopeMessage = if (requestedScope != null) {
                        " The requested scope '$requestedScope' is not available or you do not have access to it."
                    } else {
                        " No scope was requested."
                    }
                    val message = if (errorDescription.isNotBlank()) {
                        "$errorDescription.$scopeMessage"
                    } else {
                        "Invalid scope.$scopeMessage"
                    }
                    message.trim()
                }

                "invalid_grant" -> "Invalid grant: $errorDescription".trim()

                "invalid_client" -> "Invalid client credentials: $errorDescription".trim()

                "invalid_request" -> "Invalid request: $errorDescription".trim()

                else -> {
                    val baseMessage = "Maskinporten error ($errorCode)"
                    if (errorDescription.isNotBlank()) {
                        "$baseMessage: $errorDescription"
                    } else {
                        "$baseMessage (HTTP $statusCode)"
                    }
                }
            }
        } catch (ex: Exception) {
            logger.debug("Failed to parse error response as JSON", ex)
            when (statusValue) {
                400 -> "Bad request to Maskinporten. $scopeHint Response: $responseBody"
                else -> "Maskinporten returned error: $statusCode. Response: $responseBody"
            }
        }
    }

    private class ErrorResponse {
        @JsonProperty("error")
        var error: String? = null

        @JsonProperty("error_description")
        var errorDescription: String? = null
    }
}
