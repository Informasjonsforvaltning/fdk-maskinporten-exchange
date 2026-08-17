package no.digdir.fdk.maskinportenexchange.service

import no.digdir.fdk.maskinportenexchange.config.MaskinportenProperties
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class MaskinportenTokenServiceTest {

    @Mock
    private lateinit var mockClient: MaskinportenClient

    @Mock
    private lateinit var mockProperties: MaskinportenProperties

    private lateinit var tokenService: MaskinportenTokenService

    @BeforeEach
    fun setUp() {
        val placeholder = mock<MaskinportenTokenService>()
        tokenService = MaskinportenTokenService(mockClient, mockProperties, placeholder)
        val field = MaskinportenTokenService::class.java.getDeclaredField("self")
        field.isAccessible = true
        field.set(tokenService, tokenService)
    }

    @Test
    fun testGetAccessToken_Success_WithoutCache() {
        val response = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-access-token"
            tokenType = "Bearer"
            expiresIn = 3600
            scope = "altinn:serviceowners/read"
        }

        whenever(mockClient.requestToken(any())).thenReturn(response)
        whenever(mockProperties.token).thenReturn(null)
        whenever(mockProperties.scope).thenReturn("altinn:serviceowners/read")

        val accessToken = tokenService.getAccessToken()

        assertNotNull(accessToken)
        assertEquals("test-access-token", accessToken)
        verify(mockClient, times(1)).requestToken(any())
    }

    @Test
    fun testGetAccessToken_Success_WithCacheDisabled() {
        val response = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-access-token"
            expiresIn = 3600
        }

        whenever(mockClient.requestToken(any())).thenReturn(response)

        val tokenConfig = MaskinportenProperties.Token().apply {
            cache = MaskinportenProperties.Token.Cache().apply {
                enabled = false
            }
        }
        whenever(mockProperties.token).thenReturn(tokenConfig)
        whenever(mockProperties.scope).thenReturn("altinn:serviceowners/read")

        val accessToken = tokenService.getAccessToken()

        assertNotNull(accessToken)
        assertEquals("test-access-token", accessToken)
        verify(mockClient, times(1)).requestToken(any())
    }

    @Test
    fun testGetAccessToken_WithCaching_ReturnsCachedToken() {
        val response = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-access-token"
            expiresIn = 3600
        }

        whenever(mockClient.requestToken(any())).thenReturn(response)

        val tokenConfig = MaskinportenProperties.Token().apply {
            cache = MaskinportenProperties.Token.Cache().apply {
                enabled = true
                durationSeconds = 3600
            }
        }
        whenever(mockProperties.token).thenReturn(tokenConfig)
        whenever(mockProperties.scope).thenReturn("altinn:serviceowners/read")

        val spyService = spy(tokenService)
        val field = MaskinportenTokenService::class.java.getDeclaredField("self")
        field.isAccessible = true
        field.set(spyService, spyService)

        val cachedWrapper = MaskinportenTokenService.CachedTokenWrapper(response, java.time.Instant.now())
        whenever(
            spyService.getCachedTokenResponse(eq("altinn:serviceowners/read"), eq("altinn:serviceowners/read")),
        ).thenReturn(cachedWrapper)

        val token1 = spyService.getAccessToken()
        val token2 = spyService.getAccessToken()

        assertEquals(token1, token2)
        verify(mockClient, times(1)).requestToken(any())
    }

    @Test
    fun testGetAccessToken_WithCaching_UsesExpiresInFromResponse() {
        val response = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-access-token"
            expiresIn = 1800
        }

        whenever(mockClient.requestToken(any())).thenReturn(response)

        val tokenConfig = MaskinportenProperties.Token().apply {
            cache = MaskinportenProperties.Token.Cache().apply {
                enabled = true
                durationSeconds = 3600
            }
        }
        whenever(mockProperties.token).thenReturn(tokenConfig)
        whenever(mockProperties.scope).thenReturn("altinn:serviceowners/read")

        val token = tokenService.getAccessToken()

        assertNotNull(token)
        verify(mockClient, times(1)).requestToken(any())
    }

    @Test
    fun testGetAccessToken_WithCaching_UsesConfiguredDurationWhenExpiresInZero() {
        val response = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-access-token"
            expiresIn = 0
        }

        whenever(mockClient.requestToken(any())).thenReturn(response)

        val tokenConfig = MaskinportenProperties.Token().apply {
            cache = MaskinportenProperties.Token.Cache().apply {
                enabled = true
                durationSeconds = 7200
            }
        }
        whenever(mockProperties.token).thenReturn(tokenConfig)
        whenever(mockProperties.scope).thenReturn("altinn:serviceowners/read")

        val token = tokenService.getAccessToken()

        assertNotNull(token)
        verify(mockClient, times(2)).requestToken(any())
    }

    @Test
    fun testGetAccessToken_ThrowsException_WhenClientThrowsException() {
        whenever(mockClient.requestToken(anyOrNull())).thenThrow(RuntimeException("Token request failed"))
        whenever(mockProperties.token).thenReturn(null)
        whenever(mockProperties.scope).thenReturn(null)

        assertThrows(RuntimeException::class.java) { tokenService.getAccessToken() }
        verify(mockClient, times(1)).requestToken(anyOrNull())
    }

    @Test
    fun testGetAccessToken_ThrowsException_WhenResponseIsNull() {
        whenever(mockClient.requestToken(any())).thenReturn(null)
        whenever(mockProperties.token).thenReturn(null)

        assertThrows(RuntimeException::class.java) { tokenService.getAccessToken() }
    }

    @Test
    fun testGetAccessToken_ThrowsException_WhenAccessTokenIsNull() {
        val response = MaskinportenClient.TokenResponse().apply {
            accessToken = null
        }

        whenever(mockClient.requestToken(any())).thenReturn(response)
        whenever(mockProperties.token).thenReturn(null)

        assertThrows(RuntimeException::class.java) { tokenService.getAccessToken() }
    }

    @Test
    fun testClearCache() {
        assertDoesNotThrow { tokenService.clearCache() }
    }

    @Test
    fun testClearCache_AfterCaching() {
        val response = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-access-token"
            expiresIn = 3600
        }

        whenever(mockClient.requestToken(any())).thenReturn(response)

        val tokenConfig = MaskinportenProperties.Token().apply {
            cache = MaskinportenProperties.Token.Cache().apply {
                enabled = true
                durationSeconds = 3600
            }
        }
        whenever(mockProperties.token).thenReturn(tokenConfig)
        whenever(mockProperties.scope).thenReturn("altinn:serviceowners/read")

        tokenService.getAccessToken()
        tokenService.clearCache()

        val token2 = tokenService.getAccessToken()

        assertNotNull(token2)
        verify(mockClient, times(2)).requestToken(any())
    }

    @Test
    fun testGetTokenResponse_WithCaching_ReturnsScopesFromCache() {
        val response = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-access-token"
            tokenType = "Bearer"
            expiresIn = 3600
            scope = "altinn:serviceowners/read altinn:serviceowners/write"
            scopes = listOf("altinn:serviceowners/read", "altinn:serviceowners/write")
        }

        whenever(mockClient.requestToken(any())).thenReturn(response)

        val tokenConfig = MaskinportenProperties.Token().apply {
            cache = MaskinportenProperties.Token.Cache().apply {
                enabled = true
                durationSeconds = 3600
            }
        }
        whenever(mockProperties.token).thenReturn(tokenConfig)
        whenever(mockProperties.scope).thenReturn("altinn:serviceowners/read altinn:serviceowners/write")

        val spyService = spy(tokenService)
        val field = MaskinportenTokenService::class.java.getDeclaredField("self")
        field.isAccessible = true
        field.set(spyService, spyService)

        val cachedWrapper = MaskinportenTokenService.CachedTokenWrapper(response, java.time.Instant.now())
        whenever(
            spyService.getCachedTokenResponse(
                eq("altinn:serviceowners/read altinn:serviceowners/write"),
                eq("altinn:serviceowners/read altinn:serviceowners/write"),
            ),
        ).thenReturn(cachedWrapper)

        val response1 = spyService.getTokenResponse()
        assertEquals("test-access-token", response1.accessToken)
        assertEquals("altinn:serviceowners/read altinn:serviceowners/write", response1.scope)
        assertEquals(listOf("altinn:serviceowners/read", "altinn:serviceowners/write"), response1.scopes)

        val response2 = spyService.getTokenResponse()
        assertEquals("test-access-token", response2.accessToken)
        assertEquals("altinn:serviceowners/read altinn:serviceowners/write", response2.scope)
        assertEquals(listOf("altinn:serviceowners/read", "altinn:serviceowners/write"), response2.scopes)

        verify(mockClient, times(1)).requestToken(any())
    }
}
