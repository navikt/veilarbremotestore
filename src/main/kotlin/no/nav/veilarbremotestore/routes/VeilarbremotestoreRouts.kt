package no.nav.veilarbremotestore.routes

import io.ktor.application.call
import io.ktor.auth.AuthenticationRouteSelector
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.veilarbremotestore.storage.StorageProvider

fun Route.conditionalAuthenticate(useAuthentication: Boolean, build: Route.() -> Unit): Route {
    if (useAuthentication) {
        return authenticate(build = build)
    }
    val route = createChild(AuthenticationRouteSelector(listOf<String?>(null)))
    route.build()
    return route
}


fun Route.veilarbstoreRoutes(provider: StorageProvider, useAuthentication: Boolean) {
    route("/{id}") {
        get {
            call.parameters["id"]
                    ?.let {
                        val veileder = provider.hentVeilederObjekt(it)
                        if(veileder != null)
                            call.respond(veileder)
                        else{
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                    ?: call.respond(HttpStatusCode.BadRequest)
        }
        conditionalAuthenticate(useAuthentication) {
            put {
                call.parameters["id"]
                        ?.let {
                            call.respond(provider.oppdaterVeilederObjekt(call.receive(),it))
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }

            post {

                call.parameters["id"]
                        ?.let {
                            call.respond(provider.leggTilVeilederObjekt(call.receive(),it))
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }

            delete {
                call.parameters["id"]
                        ?.let {
                            provider.slettVeilederObjekt(it)
                            call.respond(HttpStatusCode.OK, "Deleted $it")
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

