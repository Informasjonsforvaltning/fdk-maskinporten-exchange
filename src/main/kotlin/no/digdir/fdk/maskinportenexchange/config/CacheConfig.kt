package no.digdir.fdk.maskinportenexchange.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class CacheConfig(
    private val properties: MaskinportenProperties
) {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager("maskinportenTokens")
        
        val cacheEnabled = properties.token?.cache?.enabled ?: false
        val durationSeconds = properties.token?.cache?.durationSeconds ?: 3600
        
        if (cacheEnabled) {
            val cacheDuration = maxOf(durationSeconds - 60, 60)
            cacheManager.setCaffeine(
                Caffeine.newBuilder()
                    .expireAfterWrite(cacheDuration.toLong(), TimeUnit.SECONDS)
                    .maximumSize(1000)
                    .recordStats()
            )
        } else {
            cacheManager.setCaffeine(
                Caffeine.newBuilder()
                    .expireAfterWrite(0, TimeUnit.SECONDS)
                    .maximumSize(0)
            )
        }
        
        return cacheManager
    }
}
