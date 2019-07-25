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
    route("/{ident}") {
        conditionalAuthenticate(useAuthentication) {
            get {
                call.parameters["ident"]
                        ?.let {
                            val veileder = provider.hentVeilederObjekt(it)
                            if(veileder != null) {
                                val params = call.request.queryParameters["keys"]?.split(",")?.map { it.trim() }
                                println(params)
                                if (params == null) {
                                    call.respond(veileder)
                                } else {
                                    val ret = veileder.filter { it.key in params }
                                    call.respond(ret)
                                }
                            }
                            else{
                                call.respond(HttpStatusCode.NoContent)
                            }
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }
            patch {
                call.parameters["ident"]
                        ?.let {
                            call.respond(provider.oppdaterVeilederFelt(call.receive(),it))
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }


            put {
                call.parameters["ident"]
                        ?.let {
                            call.respond(provider.oppdaterVeilederObjekt(call.receive(),it))
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }

            post {
                call.parameters["ident"]
                        ?.let {
                            call.respond(provider.leggTilVeilederObjekt(call.receive(),it))
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }

            delete {
                call.parameters["ident"]
                        ?.let {
                            val params = call.request.queryParameters["keys"]?.split(",")?.map { it.trim() }
                            if (params == null) {
                                provider.slettVeilederObjekt(it)
                                call.respond(HttpStatusCode.OK, "Deleted $it")
                            } else {
                                val veileder = provider.hentVeilederObjekt(it)
                                val ret = veileder?.filter { it.key in params }
                                if(ret != null){
                                    provider.slettVeilederFelter(ret, it)
                                    call.respond(HttpStatusCode.OK, "Deleted $params $it")
                                }else{
                                    call.respond(HttpStatusCode.BadRequest)
                                }
                            }
                        }
                        ?: call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

