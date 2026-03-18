package no.digdir.fdk.maskinportenexchange.controller

import no.digdir.fdk.maskinportenexchange.service.MaskinportenClient
import no.digdir.fdk.maskinportenexchange.service.MaskinportenTokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class MaskinportenControllerTest {

    @Mock
    private lateinit var tokenService: MaskinportenTokenService

    private lateinit var controller: MaskinportenController

    @BeforeEach
    fun setUp() {
        controller = MaskinportenController(tokenService)
    }

    @Test
    fun testGetToken_Success() {
        val accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.token"
        val tokenResponse = MaskinportenClient.TokenResponse().apply {
            this.accessToken = accessToken
            this.tokenType = "Bearer"
            this.expiresIn = 599
            this.scope = "difitest:test1"
        }
        whenever(tokenService.getTokenResponse(null)).thenReturn(tokenResponse)

        val response = controller.getToken(null)

        assertNotNull(response)
        assertEquals(accessToken, response.accessToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals(599, response.expiresIn)
        assertEquals("difitest:test1", response.scope)
        verify(tokenService, times(1)).getTokenResponse(null)
    }

    @Test
    fun testGetToken_WithScope() {
        val accessToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.token"
        val requestedScope = "custom:scope"
        val tokenResponse = MaskinportenClient.TokenResponse().apply {
            this.accessToken = accessToken
            this.tokenType = "Bearer"
            this.expiresIn = 599
            this.scope = requestedScope
        }
        whenever(tokenService.getTokenResponse(requestedScope)).thenReturn(tokenResponse)

        val response = controller.getToken(requestedScope)

        assertNotNull(response)
        assertEquals(accessToken, response.accessToken)
        assertEquals(requestedScope, response.scope)
        verify(tokenService, times(1)).getTokenResponse(requestedScope)
    }

    @Test
    fun testGetToken_ThrowsException() {
        val exception = RuntimeException("Token error")
        val response = controller.handleException(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.containsKey("error"))
        assertEquals("Token error", response.body!!["error"])
    }

    @Test
    fun testClearCache() {
        val response = controller.clearCache()

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(tokenService, times(1)).clearCache()
    }
}
