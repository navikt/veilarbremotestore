package no.nav.veilarbremotestore.routes

import io.ktor.application.call
import io.ktor.auth.AuthenticationRouteSelector
import io.ktor.auth.authenticate
import io.ktor.features.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import kotlinx.coroutines.isActive
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
    route("/{ident}") {
        conditionalAuthenticate(useAuthentication) {


            get {
                call.parameters["ident"]
                        ?.let { ident ->
                            provider.hentVeilederObjekt(ident)
                                    ?.let { veilederFelter ->
                                        val q = call.request.queryParameters
                                        q["ressurs"]
                                                ?.split(",")
                                                ?.map { it.trim() }
                                                ?.let { queryKeys ->
                                                    call.respond(veilederFelter.filter { felt -> felt.key in queryKeys })
                                                }
                                                ?: if(q.isEmpty()){
                                                    call.respond(veilederFelter)
                                                } else {
                                                    call.respond(HttpStatusCode.BadRequest)
                                                }
                                    }
                                    ?: call.respond(HttpStatusCode.NoContent)
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }




            patch {
                call.parameters["ident"]
                        ?.let {
                            call.respond(provider.oppdaterVeilederFelt(call.receive(), it))
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }


            put {
                call.parameters["ident"]
                        ?.let {
                            call.respond(provider.oppdaterVeilederObjekt(call.receive(), it))
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }

            post {
                call.parameters["ident"]
                        ?.let {
                            call.respond(provider.leggTilVeilederObjekt(call.receive(), it))
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }

            delete {
                call.parameters["ident"]
                        ?.let { ident ->
                            call.request.queryParameters["ressurs"]?.split(",")?.map { it.trim() }
                                    ?.let { queryParams ->
                                        val veileder = provider.hentVeilederObjekt(ident)
                                        veileder?.filter { it.key in queryParams }
                                                ?.let {
                                                    provider.slettVeilederFelter(it, ident)
                                                    call.respond(HttpStatusCode.OK, "Deleted ${it.keys} on $ident")
                                                }
                                                ?: call.respond(HttpStatusCode.BadRequest)
                                    }
                                    ?: let {
                                        provider.slettVeilederObjekt(ident)
                                        call.respond(HttpStatusCode.OK, "Deleted $it")
                                    }
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

