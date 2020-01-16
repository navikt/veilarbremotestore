package no.nav.pto.veilarbremotestore.storage

import no.nav.pto.veilarbremotestore.model.VeilederObjekt

interface StorageProvider {
    fun hentVeilederObjekt(veilederId: String): VeilederObjekt?
    fun oppdaterVeilederObjekt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt
    fun oppdaterVeilederFelt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt
    fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt
    fun slettVeilederFelter(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt
    fun slettVeilederObjekt(veilederId: String)
}
