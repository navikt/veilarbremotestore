package no.nav.veilarbremotestore.service;

import no.nav.veilarbremotestore.storage.StorageProvider
import java.util.concurrent.*;
import java.util.concurrent.Executors.*
import java.util.concurrent.TimeUnit

class AutomatiskSlettingService (provider: StorageProvider) {
    private val executor: ScheduledExecutorService = newScheduledThreadPool(1);

    init {
        executor.scheduleAtFixedRate( Test(provider), 5, 1, TimeUnit.SECONDS)
    }
}
