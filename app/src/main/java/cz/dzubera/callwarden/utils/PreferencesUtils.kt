package cz.dzubera.callwarden.utils

import android.content.Context
import cz.dzubera.callwarden.model.Credentials
import cz.dzubera.callwarden.service.IncomingCallInfoService

object PreferencesUtils {

    const val PREFERENCES_KEY = "CALL_WARDEN_PREF_KEY"
    const val DOMAIN_KEY = "DOMAIN"
    const val USER_KEY = "USER"
    const val PROJECT_ID = "PROJECT"
    const val PROJECT_NAME = "PROJECT_NAME"
    const val FIRST_START = "FIRST_START"
    const val SYNC_COUNT = "SYNC_COUNT"
    const val LAST_SYNC_DATE = "LAST_SYNC_DATE"

    // save timestamp of last sycn to shared preferences
    fun saveLastSyncDate(context: Context, value: Long) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().putLong(
            LAST_SYNC_DATE, value
        ).apply()
    }

    // load timestamp of last sync from shared preferences
    fun loadLastSyncDate(context: Context): Long {
        return context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getLong(LAST_SYNC_DATE, 0)
    }

    // save sync count value to shared preferences
    fun saveSyncCount(context: Context, value: Int) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().putInt(
            SYNC_COUNT, value
        ).apply()
    }

    // load sync count value from shared preferences
    fun loadSyncCount(context: Context): Int {
        return context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getInt(SYNC_COUNT, 100)
    }

    fun saveFirstStart(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().putBoolean(
            FIRST_START, value
        ).apply()
    }

    fun loadFirstStart(context: Context): Boolean {
        return context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getBoolean(FIRST_START, false)
    }

    fun saveProjectId(context: Context, id: String) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().putString(
            PROJECT_ID, id
        ).apply()
    }

    fun loadProjectId(context: Context): String? {
        return context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(PROJECT_ID, null)
    }

    fun loadProjectName(context: Context): String? {
        return context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(PROJECT_NAME, null)
    }

    fun saveProjectName(context: Context, name: String) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().putString(
            PROJECT_NAME, name
        ).apply()
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

    fun save(context: Context, key: String, value: String) {
        context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    fun get(context: Context, key: String): String? {
        return context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(key, null)
    }


}