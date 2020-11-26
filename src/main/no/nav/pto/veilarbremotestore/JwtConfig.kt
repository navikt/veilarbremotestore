package no.nav.pto.veilarbremotestore

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.impl.JWTParser
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import io.ktor.application.ApplicationCall
import io.ktor.auth.*
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.http.auth.*
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("veilarbremotestore.JwtConfig")

class JwtUtil {
    companion object {
        fun useJwtFromCookie(call: ApplicationCall, cookieName: String): HttpAuthHeader? {
            return try {
                val token = call.request.cookies[cookieName]
                parseAuthorizationHeader("Bearer $token")
            } catch (ex: Throwable) {
                log.error("Illegal HTTP auth header", ex)
                null
            }
        }

        fun makeJwkProvider(jwksUrl: String): JwkProvider =
                JwkProviderBuilder(URL(jwksUrl))
                        .cached(10, 24, TimeUnit.HOURS)
                        .rateLimited(10, 1, TimeUnit.MINUTES)
                        .build()

        fun validateJWT(credentials: JWTCredential, clientId: String?): Principal? {
            return try {
                log.info("1")
                requireNotNull(credentials.payload.audience) { "Audience not present" }
                log.info("2")
                if (clientId != null && clientId.isNotEmpty()) {
                    log.info("2+")
                    require(credentials.payload.audience.contains(clientId))
                }
                log.info("3")
                JWTPrincipal(credentials.payload)
            } catch (e: Exception) {
                log.error("Failed to validateJWT token" + e.message, e)
                null
            }
        }

        private fun HttpAuthHeader.getBlob() = when {
            this is HttpAuthHeader.Single -> blob
            else -> null
        }

        private fun DecodedJWT.parsePayload(): Payload {
            val payloadString = String(Base64.getUrlDecoder().decode(payload))
            return JWTParser().parsePayload(payloadString)
        }
    }
}

class MockPayload(val staticSubject: String) : Payload {
    override fun getSubject(): String {
        return staticSubject
    }

    override fun getExpiresAt(): Date {
        TODO("not implemented")
    }

    override fun getIssuer(): String {
        TODO("not implemented")
    }

    override fun getAudience(): MutableList<String> {
        TODO("not implemented")
    }

    override fun getId(): String {
        TODO("not implemented")
    }

    override fun getClaims(): MutableMap<String, Claim> {
        TODO("not implemented")
    }

    override fun getIssuedAt(): Date {
        TODO("not implemented")
    }

    override fun getClaim(name: String?): Claim {
        TODO("not implemented")
    }

    override fun getNotBefore(): Date {
        TODO("not implemented")
    }
}
