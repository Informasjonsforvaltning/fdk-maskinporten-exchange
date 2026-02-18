package com.example.fdkmaskinportenexchange.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MaskinportenPropertiesTest {

    @Test
    fun testGettersAndSetters() {
        val properties = MaskinportenProperties()
        
        properties.issuer = "https://test.maskinporten.no"
        properties.tokenEndpoint = "https://test.maskinporten.no/token"
        properties.clientId = "test-client-id"
        properties.scope = "test:scope"
        properties.keyId = "test-key-id"

        assertEquals("https://test.maskinporten.no", properties.issuer)
        assertEquals("https://test.maskinporten.no/token", properties.tokenEndpoint)
        assertEquals("test-client-id", properties.clientId)
        assertEquals("test:scope", properties.scope)
        assertEquals("test-key-id", properties.keyId)
    }

    @Test
    fun testPrivateKeyGettersAndSetters() {
        val properties = MaskinportenProperties()
        val privateKey = MaskinportenProperties.PrivateKey()
        
        privateKey.content = "-----BEGIN PRIVATE KEY-----\ntest-key-content\n-----END PRIVATE KEY-----"
        properties.privateKey = privateKey

        assertNotNull(properties.privateKey)
        assertEquals("-----BEGIN PRIVATE KEY-----\ntest-key-content\n-----END PRIVATE KEY-----", properties.privateKey!!.content)
    }

    @Test
    fun testTokenCacheGettersAndSetters() {
        val properties = MaskinportenProperties()
        val token = MaskinportenProperties.Token()
        val cache = MaskinportenProperties.Token.Cache()
        
        cache.enabled = true
        cache.durationSeconds = 3600
        token.cache = cache
        properties.token = token

        assertNotNull(properties.token)
        assertNotNull(properties.token!!.cache)
        assertTrue(properties.token!!.cache!!.enabled)
        assertEquals(3600, properties.token!!.cache!!.durationSeconds)
    }

    @Test
    fun testTokenCache_Disabled() {
        val properties = MaskinportenProperties()
        val token = MaskinportenProperties.Token()
        val cache = MaskinportenProperties.Token.Cache()
        
        cache.enabled = false
        cache.durationSeconds = 1800
        token.cache = cache
        properties.token = token

        assertFalse(properties.token!!.cache!!.enabled)
        assertEquals(1800, properties.token!!.cache!!.durationSeconds)
    }
}
