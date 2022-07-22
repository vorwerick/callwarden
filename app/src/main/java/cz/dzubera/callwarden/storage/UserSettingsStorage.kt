package cz.dzubera.callwarden.storage

class UserSettingsStorage {

    var userName = ""
    var userNumber = ""

    var callback: ((state: Int) -> Unit)? = null
}