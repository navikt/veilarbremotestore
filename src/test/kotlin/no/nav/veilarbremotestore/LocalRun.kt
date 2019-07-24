package no.nav.veilarbremotestore

import no.nav.veilarbremotestore.storage.StorageService
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("veilarbremotestore.LocalRun")

fun runLocally(useAuthentication: Boolean) {
    val applicationState = ApplicationState()
    val applicationServer = createHttpServer(
            applicationState,
            StorageService(createS3Stub()),
            7070,
            Configuration(),
            useAuthentication
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown hook called, shutting down gracefully")
        applicationState.initialized = false
        applicationServer.stop(1, 1, TimeUnit.SECONDS)
    })

    applicationServer.start(wait = true)
}

fun main() {
    runLocally(true)
}