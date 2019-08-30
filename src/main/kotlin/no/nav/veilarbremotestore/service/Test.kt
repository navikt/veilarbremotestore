package no.nav.veilarbremotestore.service

import no.nav.veilarbremotestore.storage.StorageProvider
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger("veilarbremotestore.Test")
class Test (provider: StorageProvider): Runnable {
    val provider: StorageProvider = provider;
    override fun run() {
        log.info("Starter automatiskt sletting")
        provider.slettGamleRessurser();
        log.info("Klar med slettingen")
    }
}