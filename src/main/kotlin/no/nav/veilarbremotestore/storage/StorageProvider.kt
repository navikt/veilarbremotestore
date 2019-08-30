package no.nav.veilarbremotestore.storage

import no.nav.veilarbremotestore.model.VeilederObjekt

interface StorageProvider {
    fun hentVeilederObjekt(veilederId: String): VeilederObjekt?
    fun oppdaterVeilederObjekt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt
    fun oppdaterVeilederFelt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt
    fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt
    fun slettVeilederFelter(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt
    fun slettVeilederObjekt(veilederId: String)
    fun hentRessurser(): List<String>
    fun hentRessurs(ressursNavn: String): List<String>
    fun slettGamleRessurser()
    fun leggTilRessurs(ressursNavn: String, vararg ressursVerdi: String)
    fun oppdaterRessurs(ressursNavn: String, vararg ressursVerdi: String)
    fun getAlleVeilederId(): List<String>
}
