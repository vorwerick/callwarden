package cz.dzubera.callwarden

import android.content.Context

object PreferencesUtils {

    const val PREFERENCES_KEY = "CALL_WARDEN_PREF_KEY"
    const val DOMAIN_KEY = "DOMAIN"
    const val USER_KEY = "USER"



    fun saveCredentials(context: Context, credentials:Credentials) {
        val user = context.getSharedPreferences("XXX", Context.MODE_PRIVATE).getString("userName", null)
        val number = context.getSharedPreferences("XXX", Context.MODE_PRIVATE).getString("userNumber", null)
    }

    fun loadCredentials(context: Context):Credentials? {
        val domain = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(DOMAIN_KEY, null)
        val user = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getInt(USER_KEY, -1)
        if(user != -1 && domain!=null){
            return Credentials(domain, user)
        }
        return null
    }
}