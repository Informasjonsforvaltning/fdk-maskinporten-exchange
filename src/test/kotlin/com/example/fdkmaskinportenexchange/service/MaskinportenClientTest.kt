package com.example.fdkmaskinportenexchange.service

import com.example.fdkmaskinportenexchange.config.MaskinportenProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.junit.jupiter.api.Assertions.*
import com.example.fdkmaskinportenexchange.service.MaskinportenClient.TokenResponse

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MaskinportenClientTest {

    @Mock
    private lateinit var restTemplate: RestTemplate

    @Mock
    private lateinit var properties: MaskinportenProperties

    @Mock
    private lateinit var jwtAssertionBuilder: JwtAssertionBuilder

    private val objectMapper = ObjectMapper()

    private lateinit var client: MaskinportenClient

    @BeforeEach
    fun setUp() {
        client = MaskinportenClient(restTemplate, properties, jwtAssertionBuilder, objectMapper)
        whenever(properties.tokenEndpoint).thenReturn("https://test.maskinporten.no/token")
    }

    @Test
    fun testRequestToken_Success() {
        val assertion = "test-jwt-assertion"
        val tokenResponse = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-token"
            tokenType = "Bearer"
            expiresIn = 3600
            scope = "test:scope"
        }

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenReturn(ResponseEntity(tokenResponse, HttpStatus.OK))

        val response = client.requestToken(null)

        assertNotNull(response)
        assertEquals("test-token", response.accessToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals(3600, response.expiresIn)
        assertEquals("test:scope", response.scope)
        assertEquals(listOf("test:scope"), response.scopes)
        verify(restTemplate, times(1)).postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java))
    }

    @Test
    fun testRequestToken_Success_MinimalResponse() {
        val assertion = "test-jwt-assertion"
        val tokenResponse = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-token"
        }

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenReturn(ResponseEntity(tokenResponse, HttpStatus.OK))

        val response = client.requestToken(null)

        assertNotNull(response)
        assertEquals("test-token", response.accessToken)
    }

    @Test
    fun testRequestToken_ThrowsException_WhenNon200Status() {
        val assertion = "test-jwt-assertion"

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenReturn(ResponseEntity(null, HttpStatus.BAD_REQUEST))

        assertThrows(RuntimeException::class.java) { client.requestToken(null) }
    }

    @Test
    fun testRequestToken_ThrowsException_WhenNullBody() {
        val assertion = "test-jwt-assertion"

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenReturn(ResponseEntity(null, HttpStatus.OK))

        assertThrows(RuntimeException::class.java) { client.requestToken(null) }
    }

    @Test
    fun testRequestToken_ThrowsException_WhenAccessTokenMissing() {
        val assertion = "test-jwt-assertion"
        val tokenResponse = MaskinportenClient.TokenResponse().apply {
            tokenType = "Bearer"
        }

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenReturn(ResponseEntity(tokenResponse, HttpStatus.OK))

        assertThrows(RuntimeException::class.java) { client.requestToken(null) }
    }

    @Test
    fun testRequestToken_HandlesHttpClientErrorException() {
        val assertion = "test-jwt-assertion"

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", org.springframework.http.HttpHeaders(), "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid grant\"}".toByteArray(), null))

        val exception = assertThrows(RuntimeException::class.java) { client.requestToken(null) }
        assertTrue(exception.message!!.contains("Invalid grant"))
    }

    @Test
    fun testRequestToken_HandlesInvalidScopeError() {
        val assertion = "test-jwt-assertion"
        val requestedScope = "altinn:serviceowners/read"
        val errorResponse = "{\"error\":\"invalid_scope\",\"error_description\":\"Token request contains invalid scopes for client, altinn:serviceowners/read (trace_id: bd1d0e3bfb1c7a7bd99fd447714a316c)\",\"error_uri\":\"https://test.maskinporten.no/errors/MP-200\"}"

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", org.springframework.http.HttpHeaders(), errorResponse.toByteArray(), null))

        val exception = assertThrows(RuntimeException::class.java) { client.requestToken(requestedScope) }
        assertTrue(exception.message!!.contains("Token request contains invalid scopes"))
        assertTrue(exception.message!!.contains("The requested scope '$requestedScope'") && exception.message!!.contains("may not be available"))
    }

    @Test
    fun testRequestToken_HandlesRestClientException() {
        val assertion = "test-jwt-assertion"

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenThrow(RestClientException("Connection failed"))

        val exception = assertThrows(RuntimeException::class.java) { client.requestToken(null) }
        assertTrue(exception.message!!.contains("Failed to communicate with Maskinporten"))
    }

    @Test
    fun testRequestToken_HandlesGeneralException() {
        val assertion = "test-jwt-assertion"

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenThrow(RuntimeException("Unexpected error"))

        val exception = assertThrows(RuntimeException::class.java) { client.requestToken(null) }
        assertTrue(exception.message!!.contains("Unexpected error"))
    }

    @Test
    fun testRequestToken_ParsesExpiresInCorrectly() {
        val assertion = "test-jwt-assertion"
        val tokenResponse = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-token"
            expiresIn = 7200
        }

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenReturn(ResponseEntity(tokenResponse, HttpStatus.OK))

        val response = client.requestToken(null)

        assertEquals(7200, response.expiresIn)
    }

    @Test
    fun testRequestToken_ParsesMultipleScopes() {
        val assertion = "test-jwt-assertion"
        val tokenResponse = MaskinportenClient.TokenResponse().apply {
            accessToken = "test-token"
            tokenType = "Bearer"
            expiresIn = 3600
            scope = "test:scope1 test:scope2 test:scope3"
        }

        whenever(jwtAssertionBuilder.buildJwtAssertion(any())).thenReturn(assertion)
        whenever(restTemplate.postForEntity<TokenResponse>(any<String>(), any(), eq(MaskinportenClient.TokenResponse::class.java)))
            .thenReturn(ResponseEntity(tokenResponse, HttpStatus.OK))

        val response = client.requestToken(null)

        assertNotNull(response)
        assertEquals("test-token", response.accessToken)
        assertEquals("test:scope1 test:scope2 test:scope3", response.scope)
        assertEquals(listOf("test:scope1", "test:scope2", "test:scope3"), response.scopes)
    }

    @Test
    fun testTokenResponse_GettersAndSetters() {
        val response = MaskinportenClient.TokenResponse()
        
        response.accessToken = "token"
        response.tokenType = "Bearer"
        response.expiresIn = 3600
        response.scope = "test:scope1 test:scope2"
        response.scopes = listOf("test:scope1", "test:scope2")

        assertEquals("token", response.accessToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals(3600, response.expiresIn)
        assertEquals("test:scope1 test:scope2", response.scope)
        assertEquals(listOf("test:scope1", "test:scope2"), response.scopes)
    }
}
