package cz.dzubera.callwarden

import android.content.Context

object PreferencesUtils {

    const val PREFERENCES_KEY = "CALL_WARDEN_PREF_KEY"
    const val DOMAIN_KEY = "DOMAIN"
    const val USER_KEY = "USER"
    const val PROJECT_ID = "PROJECT"

    fun saveProjectId(context: Context, id: String) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().putString(
            PROJECT_ID, id
        ).apply()
    }

    fun loadProjectId(context: Context): String? {
        return context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(PROJECT_ID, null)
    }

    fun saveCredentials(context: Context, credentials: Credentials) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().putString(
            DOMAIN_KEY, credentials.domain
        ).putInt(USER_KEY, credentials.user).apply()
    }

    fun loadCredentials(context: Context): Credentials? {
        val domain = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(DOMAIN_KEY, null)
        val user = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getInt(USER_KEY, -1)
        if (user != -1 && domain != null) {
            return Credentials(domain, user)
        }
        return null
    }

    fun clearCredentials(context: Context) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().clear().apply()
    }
}