package no.nav.pto.veilarbremotestore

import com.auth0.jwk.JwkProvider
import com.natpryce.konfig.*

private const val secret = ""
private const val notUsedLocally = ""
private val defaultProperties = ConfigurationMap(
        mapOf(
                "NAIS_CLUSTER_NAME" to notUsedLocally,
                "S3_ACCESS_KEY" to secret,
                "S3_SECRET_KEY" to secret,
                "S3_URL" to notUsedLocally,
                "S3_REGION" to notUsedLocally,
                "ISSO_JWKS_URL" to "https://isso-q.adeo.no/isso/oauth2/connect/jwk_uri",
                "ISSO_ISSUER" to "https://isso-q.adeo.no:443/isso/oauth2",
                "NAMESPACE" to "q",
                "AZUREAD_ISSUER" to notUsedLocally,
                "AZUREAD_JWKS_URL" to notUsedLocally
        )
)

data class Configuration(
        val clusterName: String = config()[Key("NAIS_CLUSTER_NAME", stringType)],
        val s3Url: String = config()[Key("S3_URL", stringType)],
        val s3Region: String = config()[Key("S3_REGION", stringType)],
        val s3AccessKey: String = config()[Key("S3_ACCESS_KEY", stringType)],
        val s3SecretKey: String = config()[Key("S3_SECRET_KEY", stringType)],
        val issoJwksUrl: JwkProvider = JwtUtil.makeJwkProvider(config()[Key("ISSO_JWKS_URL", stringType)]),
        val issoJwtIssuer: String = config()[Key("ISSO_ISSUER", stringType)],
        val namespace: String = config()[Key("NAMESPACE", stringType)],
        val azureAdJwksUrl: JwkProvider = JwtUtil.makeJwkProvider(config()[Key("AZUREAD_JWKS_URL", stringType)]),
        val azureAdClientId: String = config()[Key("VEILARBLOGIN_AAD_CLIENT_ID", stringType)]
)

private fun config() = ConfigurationProperties.systemProperties() overriding
        EnvironmentVariables overriding
        defaultProperties
