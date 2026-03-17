package no.digdir.fdk.maskinportenexchange.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "maskinporten")
class MaskinportenProperties {
    var issuer: String? = null
    var tokenEndpoint: String? = null
    var clientId: String? = null
    var scope: String? = null
    var keyId: String? = null
    var privateKey: PrivateKey? = null
    var token: Token? = null

    class PrivateKey {
        var content: String? = null
    }

    class Token {
        var cache: Cache? = null

        class Cache {
            var enabled: Boolean = false
            var durationSeconds: Int = 0
        }
    }
}
