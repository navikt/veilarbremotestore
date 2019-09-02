package no.nav.veilarbremotestore.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode

import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.veilarbremotestore.storage.StorageProvider

fun Route.internalRoutes(provider: StorageProvider) {
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
                    ?.let { call.respond(provider.oppdaterVeilederFelt(call.receive(), veilederId))  }
                    ?: call.respond(HttpStatusCode.NotFound)
        }
        delete("/{veilederId}") {
            val veilederId = call.parameters["veilederId"]
            veilederId
                    ?.let { call.respond(provider.slettVeilederFelter(call.receive(), veilederId))  }
                    ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}