package cz.dzubera.callwarden.utils

import android.content.Context
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.service.HttpRequest
import cz.dzubera.callwarden.service.db.CallEntity
import io.sentry.Sentry
import org.json.JSONArray
import org.json.JSONObject

fun uploadCall(context: Context?, callEntities: List<CallEntity>, success: (Boolean) -> Unit) {
    val credentials = context?.let { PreferencesUtils.loadCredentials(it) }
    if (credentials != null) {
        val recordJson = JSONObject()
        val jsonArray = JSONArray()
        callEntities.forEach { callEntity ->
            var coutnryCode = "CZ"
            Iso2Phone.all.forEach { (k, v) ->
                if (callEntity.phoneNumber != null && callEntity.phoneNumber.contains(v)) {
                    coutnryCode = k
                }
            }

            val recordItem = JSONObject()
            recordItem.put("projectId", callEntity.projectId)
            recordItem.put("projectIdOld", callEntity.projectIdOld)
            recordItem.put("direction", Call.Direction.valueOf(callEntity.direction ?: "").ordinal)
            recordItem.put("number", callEntity.phoneNumber)
            recordItem.put("startTimestamp", callEntity.callStarted)
            recordItem.put("connectTimestamp", callEntity.callAccepted)

            recordItem.put("endTimestamp", callEntity.callEnded)
            recordItem.put("callDuration", callEntity.callDuration)
            recordItem.put("countryCode", coutnryCode)

            jsonArray.put(recordItem)

        }

        recordJson.put("records", jsonArray)
        HttpRequest.sendEntries(
            credentials.domain,
            credentials.user,
            recordJson.toString()
        ) { httpResponse ->
            when (httpResponse.code) {
                200 -> {
                    success(true)
                }
                422 -> {
                    success(true)
                    HttpRequest.getProjects(credentials.domain, credentials.user) {}
                }
                else -> {
                    Sentry.addBreadcrumb("Upload call error, status code " + httpResponse.code)
                    success(false)
                }
            }

        }
    }

}
