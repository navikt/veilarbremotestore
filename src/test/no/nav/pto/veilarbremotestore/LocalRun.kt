package no.nav.veilarbremotestore

import no.nav.pto.veilarbremotestore.ApplicationState
import no.nav.pto.veilarbremotestore.Configuration
import no.nav.pto.veilarbremotestore.createHttpServer
import no.nav.pto.veilarbremotestore.storage.StorageService
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("veilarbremotestore.LocalRun")

fun runLocally(useAuthentication: Boolean) {
    val applicationState = ApplicationState()
    val applicationServer = createHttpServer(
            applicationState,
            StorageService(createS3Stub(),"local"),
            7070,
            Configuration(),
            useAuthentication
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown hook called, shutting down gracefully")
        applicationState.initialized = false
        applicationServer.stop(1000, 1000)
    })

    applicationServer.start(wait = true)
}

fun main() {
    runLocally(true)
}
