package no.nav.veilarbremotestore.storage

import no.nav.veilarbremotestore.model.VeilederObjekt
import no.nav.veilarbremotestore.model.Veiledere;

interface StorageProvider {
    fun hentVeildere(): Veiledere
    fun hentVeilederObjekt(veilederId: String): VeilederObjekt
    fun oppdaterVeilederObjekt(veilederObjekt: VeilederObjekt): VeilederObjekt
    fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt): VeilederObjekt
    fun slettVeilederObjekt(id: String)
}
