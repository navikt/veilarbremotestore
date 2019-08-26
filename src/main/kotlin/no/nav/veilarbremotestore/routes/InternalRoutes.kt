package no.nav.veilarbremotestore.routes

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.veilarbremotestore.storage.StorageProvider

fun Route.internalRoutes(provider: StorageProvider) {
    route("/internal") {

            get("{resourceName}") {
                val resourceName = call.parameters["resourceName"]
                call.respond(provider.oppdaterVeilederFelt(call.receive(), ident))
            }

            post("{resourceName}") {
                val resourceName = call.parameters["resourceName"]
                call.respond(provider.leggTilRessurs(call.receive(), ident))
            }

            put("{resourceName}") {
                val resourceName = call.parameters["resourceName"]
                call.respond(provider.oppdaterRessurs(call.receive(), ident))
            }
    }
}
