package no.nav.veilarbremotestore.storage

import no.nav.veilarbremotestore.model.VeilederObjekt

interface StorageProvider {
    fun hentVeilederObjekt(veilederId: String): VeilederObjekt?
    fun oppdaterVeilederObjekt(veilederObjekt: VeilederObjekt, id: String): VeilederObjekt
    fun oppdaterVeilederFelt(veilederObjekt: VeilederObjekt, id: String): VeilederObjekt
    fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt, id: String): VeilederObjekt
    fun slettVeilederFelter(veilederObjekt: VeilederObjekt, id: String): VeilederObjekt
    fun slettVeilederObjekt(id: String)
    fun leggTilRessurs(id: String)
    fun oppdaterRessurs(id: String)
    fun hentRessurs(id: String)
}
