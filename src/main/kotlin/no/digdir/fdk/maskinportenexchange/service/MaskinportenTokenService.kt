package no.digdir.fdk.maskinportenexchange.service

import no.digdir.fdk.maskinportenexchange.config.MaskinportenProperties
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MaskinportenTokenService(
    private val client: MaskinportenClient,
    private val properties: MaskinportenProperties,
    @param:Lazy private val self: MaskinportenTokenService
) {
    private val logger = LoggerFactory.getLogger(MaskinportenTokenService::class.java)

    @Throws(Exception::class)
    fun getAccessToken(scope: String? = null): String {
        return getTokenResponse(scope).accessToken 
            ?: throw IllegalStateException("Access token is null")
    }

    @Throws(Exception::class)
    fun getTokenResponse(scope: String? = null): MaskinportenClient.TokenResponse {
        val normalizedScope = normalizeScope(scope ?: properties.scope)
        val cacheKey = normalizedScope ?: "default"
        
        return if (isCacheEnabled()) {
            val cachedWrapper = self.getCachedTokenResponse(cacheKey, normalizedScope)
            val updatedResponse = updateExpiresIn(cachedWrapper)
            
            if (updatedResponse.expiresIn <= 0) {
                logger.debug("Token expired (expires_in: {}), evicting from cache and fetching new token", updatedResponse.expiresIn)
                evictFromCache(cacheKey)
                val newResponse = client.requestToken(normalizedScope)
                if (newResponse.expiresIn > 0) {
                    self.getCachedTokenResponse(cacheKey, normalizedScope)
                }
                newResponse
            } else {
                updatedResponse
            }
        } else {
            logger.debug("Cache disabled, fetching new token from Maskinporten for scope: {}", normalizedScope)
            client.requestToken(normalizedScope)
        }
    }

    @Cacheable(
        cacheNames = ["maskinportenTokens"],
        key = "#cacheKey",
        unless = "#result == null || #result.tokenResponse.expiresIn <= 0"
    )
    fun getCachedTokenResponse(cacheKey: String, scope: String?): CachedTokenWrapper {
        logger.debug("Cache miss, fetching new token from Maskinporten for scope: {}", scope)
        val response = client.requestToken(scope)
        
        if (response.expiresIn <= 0) {
            logger.warn("Received token with expires_in: {} - token is already expired, will not cache", response.expiresIn)
        }
        
        return CachedTokenWrapper(response, Instant.now())
    }

    private fun updateExpiresIn(wrapper: CachedTokenWrapper): MaskinportenClient.TokenResponse {
        val response = wrapper.tokenResponse
        val cachedAt = wrapper.cachedAt
        val now = Instant.now()
        
        val elapsedSeconds = now.epochSecond - cachedAt.epochSecond
        val originalExpiresIn = response.expiresIn
        val remainingSeconds = maxOf(originalExpiresIn - elapsedSeconds.toInt(), 0)
        
        val updatedResponse = MaskinportenClient.TokenResponse().apply {
            accessToken = response.accessToken
            tokenType = response.tokenType
            expiresIn = remainingSeconds
            scope = response.scope
            scopes = response.scopes
        }
        
        logger.debug(
            "Updated expires_in from {} to {} seconds (elapsed: {} seconds)",
            originalExpiresIn, remainingSeconds, elapsedSeconds
        )
        
        return updatedResponse
    }

    private fun isCacheEnabled(): Boolean {
        return properties.token?.cache?.enabled == true
    }

    private fun normalizeScope(scope: String?): String? {
        if (scope.isNullOrBlank()) {
            return scope
        }

        return scope
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
    }

    @Caching(evict = [CacheEvict(cacheNames = ["maskinportenTokens"], allEntries = true)])
    fun clearCache() {
        logger.info("Token cache cleared")
    }

    @CacheEvict(cacheNames = ["maskinportenTokens"], key = "#cacheKey")
    fun evictFromCache(cacheKey: String) {
        logger.debug("Evicting token from cache for key: {}", cacheKey)
    }

    data class CachedTokenWrapper(
        val tokenResponse: MaskinportenClient.TokenResponse,
        val cachedAt: Instant
    )
}
