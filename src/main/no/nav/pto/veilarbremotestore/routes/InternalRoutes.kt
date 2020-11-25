package no.nav.pto.veilarbremotestore.routes

import io.ktor.application.call
import io.ktor.http.*

import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import no.nav.pto.veilarbremotestore.storage.StorageProvider

fun Route.internalRoutes(
    provider: StorageProvider, readinessCheck: () -> Boolean,
    livenessCheck: () -> Boolean = { true },
    collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
) {
    route("/internal") {
        get("/{veilederId}") {
            val veilederId = call.parameters["veilederId"]
            veilederId
                ?.let { provider.hentVeilederObjekt(veilederId)?.let { it1 -> call.respond(it1) } }
                ?: call.respond(HttpStatusCode.NotFound)
        }
        put("/{veilederId}") {
            val veilederId = call.parameters["veilederId"]
            veilederId
                ?.let { call.respond(provider.oppdaterVeilederFelt(call.receive(), veilederId)) }
                ?: call.respond(HttpStatusCode.NotFound)
        }
        delete("/{veilederId}") {
            val veilederId = call.parameters["veilederId"]
            veilederId
                ?.let { call.respond(provider.slettVeilederFelter(call.receive(), veilederId)) }
                ?: call.respond(HttpStatusCode.NotFound)
        }

        get("/isAlive") {
            if (livenessCheck()) {
                call.respondText("Alive")
            } else {
                call.respondText("Not alive", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/isReady") {
            if (readinessCheck()) {
                call.respondText("Ready")
            } else {
                call.respondText("Not ready", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/metrics") {
            val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
            call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
            }
        }
    }
}
