package com.example.fdkmaskinportenexchange.service

import com.example.fdkmaskinportenexchange.config.MaskinportenProperties
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Component
import java.io.IOException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPrivateKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

@Component
class JwtAssertionBuilder(
    private val properties: MaskinportenProperties
) {
    private var privateKey: RSAPrivateKey? = null

    @Throws(JOSEException::class, IOException::class, NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun buildJwtAssertion(scope: String? = null): String {
        val key = getPrivateKey()
        val now = Instant.now()
        val expiry = now.plusSeconds(120)

        val issuer = properties.issuer ?: throw IllegalArgumentException("Issuer must be configured")
        val issuerAudience = if (issuer.endsWith("/")) issuer else "$issuer/"

        val effectiveScope = normalizeScope(scope ?: properties.scope)

        val claimsSet = JWTClaimsSet.Builder()
            .issuer(properties.clientId ?: throw IllegalArgumentException("Client ID must be configured"))
            .audience(issuerAudience)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiry))
            .jwtID(UUID.randomUUID().toString())
            .claim("scope", effectiveScope)
            .build()

        val headerBuilder = JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
        
        properties.keyId?.takeIf { it.isNotEmpty() }?.let {
            headerBuilder.keyID(it)
        }
        
        val signedJWT = SignedJWT(headerBuilder.build(), claimsSet)
        signedJWT.sign(RSASSASigner(key))

        return signedJWT.serialize()
    }

    @Throws(IOException::class, NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun getPrivateKey(): RSAPrivateKey {
        return privateKey ?: loadPrivateKey().also { privateKey = it }
    }

    @Throws(IOException::class, NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun loadPrivateKey(): RSAPrivateKey {
        val keyContent = properties.privateKey?.content 
            ?: throw IllegalArgumentException("Private key content must be configured")
        
        val cleanedKeyContent = keyContent
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")

        val keyBytes = Base64.getDecoder().decode(cleanedKeyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        
        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
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
}
