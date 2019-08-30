package no.nav.veilarbremotestore.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.veilarbremotestore.storage.StorageProvider
import org.apache.http.HttpStatus

fun Route.internalRoutes(provider: StorageProvider) {
    route("/internal") {

        get("{resourceName}") {
            val resourceName = call.parameters["resourceName"]
            resourceName
                    ?.let { call.respond(provider.hentRessurs(resourceName))  }
                    ?: call.respond(HttpStatus.SC_NOT_FOUND)
        }
        delete {
            call.respond(provider.slettGamleRessurser())
        }

        get {
            call.respond(emptyList<String>().size)
        }

        post {
            call.respond(provider.leggTilRessurs(call.receive()))
        }

        put {
            call.respond(provider.oppdaterRessurs(call.receive()))
        }
    }
}
