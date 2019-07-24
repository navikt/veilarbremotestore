package no.nav.modiapersonoversikt.routes

import io.ktor.application.call
import io.ktor.auth.AuthenticationRouteSelector
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.modiapersonoversikt.storage.StorageProvider
import java.util.*

fun Route.conditionalAuthenticate(useAuthentication: Boolean, build: Route.() -> Unit): Route {
    if (useAuthentication) {
        return authenticate(build = build)
    }
    val route = createChild(AuthenticationRouteSelector(listOf<String?>(null)))
    route.build()
    return route
}


fun Route.skrivestotteRoutes(provider: StorageProvider, useAuthentication: Boolean) {

    route("/skrivestotte") {
        get {
            call.respond(provider.hentVeilederObjekt())
        }

        conditionalAuthenticate(useAuthentication) {
            put {
                call.respond(provider.oppdaterVeilederObjekt(call.receive()))
            }

            post {
                call.respond(provider.leggTilVeilederObjekt(call.receive()))
            }

            delete("/{id}") {
                call.parameters["id"]
                        ?.let {
                            provider.slettVeilederObjekt(UUID.fromString(it))
                            call.respond(HttpStatusCode.OK, "Deleted $it")
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}
