package no.nav.pto.veilarbremotestore

import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.auth.*
import io.ktor.response.*
import org.slf4j.LoggerFactory
import java.net.URL
import java.security.interfaces.RSAPublicKey

private val log = LoggerFactory.getLogger("veilarbremotestore.JwtAuth")

fun AuthenticationPipeline.bearerAuthentication(realm: String) {
    intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        log.info("Try this")
        // parse token
        val authHeader = call.request.parseAuthorizationHeader()
        val jwt: DecodedJWT? =
                if (authHeader?.authScheme == "Bearer" && authHeader is HttpAuthHeader.Single) {
                    try {
                        verifyToken(authHeader.blob)
                    } catch (e: Exception) {
                        null
                    }
                } else null

        // transform token to principal
        val principal = jwt?.let { UserIdPrincipal(jwt.subject ?: jwt.getClaim("NAVident").asString()) }

        // set principal if success
        if (principal != null) {
            context.principal(principal)
        } else {
            log.warn("No principal")
            call.respond(UnauthorizedResponse(HttpAuthHeader.bearerAuthChallenge(realm)))
        }
    }
}

private fun HttpAuthHeader.Companion.bearerAuthChallenge(realm: String): HttpAuthHeader = HttpAuthHeader.Parameterized("Bearer", mapOf(HttpAuthHeader.Parameters.Realm to realm))

fun verifyToken(token: String): DecodedJWT {
    val jwkProvider = UrlJwkProvider(URL("https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0/.well-known/openid-configuration"))

    val jwt = JWT.decode(token)
    val jwk = jwkProvider.get(jwt.keyId)

    //val publicKey: RSAPublicKey = jwk.publicKey as RSAPublicKey // unsafe
    val publicKey = jwk.publicKey as? RSAPublicKey ?: throw Exception("Invalid key type") // safe

    val algorithm = when (jwk.algorithm) {
        "RS256" -> Algorithm.RSA256(publicKey, null)
        else -> throw Exception("Unsupported algorithm")
    }

    val verifier = JWT.require(algorithm) // signature
            .withIssuer("https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0") // iss
            .build()

    return verifier.verify(token)
}