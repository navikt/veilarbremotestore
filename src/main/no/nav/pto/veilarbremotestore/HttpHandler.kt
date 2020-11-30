package no.nav.pto.veilarbremotestore

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.jwt
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.jackson.JacksonConverter
import io.ktor.metrics.dropwizard.DropwizardMetrics
import io.ktor.request.path
import io.ktor.routing.*
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.dropwizard.DropwizardExports
import no.nav.pto.veilarbremotestore.JwtUtil.Companion.useJwtFromCookie
import no.nav.pto.veilarbremotestore.ObjectMapperProvider.Companion.objectMapper
import no.nav.pto.veilarbremotestore.routes.internalRoutes
import no.nav.pto.veilarbremotestore.routes.veilarbstoreRoutes
import no.nav.pto.veilarbremotestore.storage.StorageProvider

fun createHttpServer(
    applicationState: ApplicationState,
    provider: StorageProvider,
    port: Int = 7070,
    configuration: Configuration,
    useAuthentication: Boolean = true
): ApplicationEngine = embeddedServer(Netty, port) {

    install(StatusPages) {
        notFoundHandler()
        exceptionHandler()
    }

    install(CORS) {
        anyHost()
        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)

        allowCredentials = true
    }


    install(Authentication) {
        jwt("AzureAD") {
            skipWhen { applicationCall -> applicationCall.request.cookies[AuthCookies.AZURE_AD.cookieName] == null }
            realm = "veilarbremotestore"
            authHeader { applicationCall ->
                useJwtFromCookie(
                        applicationCall,
                        AuthCookies.AZURE_AD.cookieName
                )
            }

            verifier(configuration.azureAdJwksUrl)
            validate { JwtUtil.validateJWT(it, configuration.azureAdClientId) }
        }
        jwt("OpenAM") {
            skipWhen { applicationCall -> applicationCall.request.cookies[AuthCookies.OPEN_AM.cookieName] == null }
            realm = "veilarbremotestore"
            authHeader { applicationCall ->
                useJwtFromCookie(
                        applicationCall,
                        AuthCookies.OPEN_AM.cookieName
                )
            }
            verifier(configuration.issoJwksUrl, configuration.issoJwtIssuer)
            validate { JwtUtil.validateJWT(it, null) }
        }

    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    /*install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().contains("/internal")}
        //mdc("userId") { applicationCall -> applicationCall.getNavident() }
    }*/

    install(DropwizardMetrics) {
        CollectorRegistry.defaultRegistry.register(DropwizardExports(registry))
    }

    routing {
        route("veilarbremotestore") {
            veilarbstoreRoutes(provider, useAuthentication)
            internalRoutes(provider, readinessCheck = { applicationState.initialized }, livenessCheck = { applicationState.running })
        }
    }

    applicationState.initialized = true
}
