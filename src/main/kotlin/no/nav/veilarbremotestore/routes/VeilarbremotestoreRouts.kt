package no.nav.veilarbremotestore.routes

import io.ktor.application.call
import io.ktor.auth.AuthenticationRouteSelector
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.veilarbremotestore.JwtUtil.Companion.getSubject
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
    route("/") {
        conditionalAuthenticate(useAuthentication) {
            get {
                val ident = getSubject(call)
                provider.hentVeilederObjekt(ident)
                        ?.let { veilederFelter ->
                            val q = call.request.queryParameters
                            q["ressurs"]
                                    ?.split(",")
                                    ?.map { it.trim() }
                                    ?.let { queryKeys ->
                                        call.respond(veilederFelter.filter { felt -> felt.key in queryKeys })
                                    }
                                    ?: if (q.isEmpty()) {
                                        call.respond(veilederFelter)
                                    } else {
                                        call.respond(HttpStatusCode.BadRequest)
                                    }
                        }
                        ?: call.respond(HttpStatusCode.NoContent)

            }

            patch {
                val ident = getSubject(call)
                call.respond(provider.oppdaterVeilederFelt(call.receive(), ident))

            }


            put {
                val ident = getSubject(call)
                call.respond(provider.oppdaterVeilederObjekt(call.receive(), ident))

            }

            post {
                val ident = getSubject(call)
                call.respond(provider.leggTilVeilederObjekt(call.receive(), ident))

            }

            delete {
                val ident = getSubject(call)
                provider.hentVeilederObjekt(ident)
                        ?.let { veilederFelter ->
                            val q = call.request.queryParameters
                            q["ressurs"]
                                    ?.split(",")
                                    ?.map { it.trim() }
                                    ?.let { queryKeys ->
                                        val toBeDeleted = veilederFelter.filter { felt -> felt.key in queryKeys }
                                        provider.slettVeilederFelter(toBeDeleted, ident)
                                        call.respond(HttpStatusCode.OK, "Deleted ${queryKeys} on $ident")
                                    }
                                    ?: if (q.isEmpty()) {
                                        provider.slettVeilederObjekt(ident)
                                        call.respond(HttpStatusCode.OK, "Deleted $ident")
                                    } else {
                                        call.respond(HttpStatusCode.BadRequest)
                                    }
                        }
                        ?: call.respond(HttpStatusCode.NoContent)

            }
        }
    }
}

