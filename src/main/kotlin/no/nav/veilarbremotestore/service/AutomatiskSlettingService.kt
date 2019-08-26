package no.nav.veilarbremotestore.service;

import java.util.concurrent.*;


class AutomatiskSlettingService (provider: StorageProvider) {
        init {
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleWithFixedDelay(() -> {
                provider.hentRessurs("endringslogg")?.let
            });

        }
}
