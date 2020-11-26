package no.nav.pto.veilarbremotestore

enum class AuthCookies(val cookieName: String) {
    OPEN_AM("ID_token"),
    AZURE_AD("isso-idtoken")
}