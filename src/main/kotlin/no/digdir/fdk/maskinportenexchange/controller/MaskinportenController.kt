package no.digdir.fdk.maskinportenexchange.controller

import no.digdir.fdk.maskinportenexchange.service.MaskinportenClient
import no.digdir.fdk.maskinportenexchange.service.MaskinportenTokenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/maskinporten")
@Tag(name = "Maskinporten", description = "API for obtaining and managing Maskinporten access tokens")
class MaskinportenController(
    private val tokenService: MaskinportenTokenService
) {
    private val logger = LoggerFactory.getLogger(MaskinportenController::class.java)

    @GetMapping("/token")
    @Operation(
        summary = "Get access token",
        description = "Obtains a JWT access token from Maskinporten using client credentials flow. " +
                "Optionally accepts a scope parameter to request one or more scopes. " +
                "Multiple scopes can be provided as space-separated or comma-separated values (e.g., 'scope1 scope2' or 'scope1, scope2')."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token obtained successfully",
                content = [Content(schema = Schema(implementation = MaskinportenClient.TokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = [Content(schema = Schema(implementation = Map::class))]
            )
        ]
    )
    fun getToken(
        @Parameter(
            description = "Space-separated or comma-separated list of scopes to request. " +
                    "Examples: 'altinn:serviceowner', 'altinn:serviceowner altinn:serviceowner/rolesandrights', or 'altinn:serviceowner, altinn:serviceowner/rolesandrights'",
            example = "altinn:serviceowner altinn:serviceowner/rolesandrights",
            required = false
        )
        @RequestParam(required = false) scope: String?
    ): MaskinportenClient.TokenResponse {
        return tokenService.getTokenResponse(scope)
    }

    @PostMapping("/cache/clear")
    @Operation(
        summary = "Clear token cache",
        description = "Clears the cached access tokens, forcing a new token request on next call"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Cache cleared successfully"
            )
        ]
    )
    fun clearCache(): ResponseEntity<Unit> {
        tokenService.clearCache()
        return ResponseEntity.ok().build()
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Map<String, String>> {
        logger.error("Failed to retrieve token", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to (e.message ?: "Unknown error")))
    }
}
