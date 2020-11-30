package no.nav.pto.veilarbremotestore.routes

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.*
import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.pto.veilarbremotestore.AuthCookies
import no.nav.pto.veilarbremotestore.JwtUtil
import no.nav.pto.veilarbremotestore.MockPayload
import no.nav.pto.veilarbremotestore.storage.StorageProvider
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("veilarbremotestore.veilarbstoreRoutes")

fun Route.conditionalAuthenticate(useAuthentication: Boolean, build: Route.() -> Unit): Route {
    if (useAuthentication) {
        val authenticate = authenticate(build = build, configurations = arrayOf("AzureAD", "OpenAM"))
        authenticate.intercept(Authentication.AuthenticatePhase){
            this.context.authentication.allErrors.forEach{ error->
                log.info(error.message)
            }
        }
        return authenticate
    }
    val route = createChild(AuthenticationRouteSelector(listOf<String?>(null)))
    route.insertPhaseAfter(ApplicationCallPipeline.Features, Authentication.AuthenticatePhase)
    route.intercept(Authentication.AuthenticatePhase) {
        this.context.authentication.principal = JWTPrincipal(MockPayload("Z999999"))
    }
    route.build()
    return route
}


fun Route.veilarbstoreRoutes(provider: StorageProvider, useAuthentication: Boolean) {
    route("/") {
        conditionalAuthenticate(useAuthentication) {
            get("") {
                val ident = call.getNavident()
                ident?.let { navIdent ->
                    provider.hentVeilederObjekt(navIdent)
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
            }

            patch {
                val ident = call.getNavident()
                ident?.let {
                    call.respond(provider.oppdaterVeilederFelt(call.receive(), ident))
                }
            }


            put {
                val ident = call.getNavident()
                ident?.let {
                    call.respond(provider.oppdaterVeilederObjekt(call.receive(), ident))
                }
            }

            post {
                val ident = call.getNavident()
                ident?.let {
                    call.respond(provider.leggTilVeilederObjekt(call.receive(), ident))
                }
            }

            delete {
                val ident = call.getNavident()
                ident?.let {
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
    handle {
        this.context.authentication.allErrors.forEach{ error->
            log.info(error.message)
        }
    }
}


fun ApplicationCall.getNavident(): String? {
    if (this.principal<JWTPrincipal>()?.payload != null &&
            this.principal<JWTPrincipal>()?.payload?.claims != null &&
            this.principal<JWTPrincipal>()?.payload?.claims?.containsKey("NAVident")!!) {
        log.info("NAV IDENT")
        return this.principal<JWTPrincipal>()?.payload?.getClaim("NAVident")?.asString();
    }
    return this.principal<JWTPrincipal>()
            ?.payload
            ?.subject
}

