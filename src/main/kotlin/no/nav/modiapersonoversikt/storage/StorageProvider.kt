package no.nav.modiapersonoversikt.storage

import no.nav.modiapersonoversikt.model.VeilederObjekt
import no.nav.modiapersonoversikt.model.Veiledere;

import java.util.*

interface StorageProvider {
    fun hentVeildere(): Veiledere
    fun hentVeilederObjekt(veilederId: String): VeilederObjekt
    fun oppdaterVeilederObjekt(veilederObjekt: VeilederObjekt): VeilederObjekt
    fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt): VeilederObjekt
    fun slettVeilederObjekt(id: String)
}
