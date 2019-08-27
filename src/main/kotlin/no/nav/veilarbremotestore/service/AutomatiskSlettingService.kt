package no.nav.veilarbremotestore.service;

import no.nav.veilarbremotestore.storage.StorageProvider
import java.util.concurrent.*;
import java.util.concurrent.Executors.*
import java.util.concurrent.TimeUnit


class AutomatiskSlettingService (provider: StorageProvider) {
    private val executor: ScheduledExecutorService = newScheduledThreadPool(1);

    init {
        executor.scheduleWithFixedDelay({ this.slettEndringslogg()}, 0, 7, TimeUnit.DAYS)
    }

    private fun slettEndringslogg() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
