package no.nav.veilarbremotestore.storage

import no.nav.veilarbremotestore.model.VeilederObjekt

interface StorageProvider {
    fun hentVeilederObjekt(veilederId: String): VeilederObjekt?
    fun oppdaterVeilederObjekt(veilederObjekt: VeilederObjekt, id: String): VeilederObjekt
    fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt, id: String): VeilederObjekt
    fun slettVeilederObjekt(id: String)
}
