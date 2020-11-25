package no.nav.pto.veilarbremotestore.routes

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Route
import io.ktor.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

fun Route.naisRoutes(readinessCheck: () -> Boolean,
                     livenessCheck: () -> Boolean = { true },
                     collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry) {


}
