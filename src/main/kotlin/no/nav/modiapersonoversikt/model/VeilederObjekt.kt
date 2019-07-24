package no.nav.modiapersonoversikt.model

typealias Veiledere = Map<String, VeilederObjekt>

data class VeilederObjekt (
        val innhold: Map<String, String>,
        val id: String
)
