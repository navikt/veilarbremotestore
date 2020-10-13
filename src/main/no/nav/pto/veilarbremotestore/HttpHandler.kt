package no.nav.pto.veilarbremotestore

import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.jackson.JacksonConverter
import io.ktor.metrics.dropwizard.DropwizardMetrics
import io.ktor.request.path
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.dropwizard.DropwizardExports
import no.nav.pto.veilarbremotestore.ObjectMapperProvider.Companion.objectMapper
import no.nav.pto.veilarbremotestore.routes.internalRoutes
import no.nav.pto.veilarbremotestore.routes.naisRoutes
import no.nav.pto.veilarbremotestore.routes.veilarbstoreRoutes
import no.nav.pto.veilarbremotestore.storage.StorageProvider
import org.slf4j.event.Level

fun createHttpServer(applicationState: ApplicationState,
                     provider: StorageProvider,
                     port: Int = 7070,
                     configuration: Configuration,
                     useAuthentication: Boolean = true): ApplicationEngine = embeddedServer(Netty, port) {

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

    if (useAuthentication) {
        install(Authentication) {
            jwt {
                authHeader(JwtUtil.Companion::useJwtFromCookie)
                realm = "veilarbremotestore"
                verifier(configuration.jwksUrl, configuration.jwtIssuer)
                validate { JwtUtil.validateJWT(it) }
            }
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/veilarbremotestore") }
        mdc("userId", JwtUtil.Companion::getSubject)
    }

    install(DropwizardMetrics) {
        CollectorRegistry.defaultRegistry.register(DropwizardExports(registry))
    }

    routing {
        route("veilarbremotestore") {
            naisRoutes(readinessCheck = { applicationState.initialized }, livenessCheck = { applicationState.running })
            internalRoutes(provider)
            veilarbstoreRoutes(provider, useAuthentication)
        }
    }

    applicationState.initialized = true
}