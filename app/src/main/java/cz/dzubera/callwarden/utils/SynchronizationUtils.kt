package cz.dzubera.callwarden.utils

import android.content.Context
import android.util.Log
import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.model.CallHistory
import cz.dzubera.callwarden.service.db.CallEntity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun startSynchronization(context: Context, state: ((String) -> Unit)?) {
    val calls = CallHistory.getCallsHistory(
        context,
        PreferencesUtils.loadSyncCount(context)
    ).toMutableList()
    GlobalScope.launch {
        val callsFromDB = App.appDatabase.taskCalls().getAll()

        val syncCalls = mutableListOf<CallEntity>()
        val uiCalls = mutableListOf<Call>()
        calls.forEach {
            if (callsFromDB.any { entityDb -> entityDb.uid == it.callStartedTimestamp }) {
                return@forEach
            }
            //log callstarted
            Log.d("Need to be synchronized", it.callStartedTimestamp.toString())

            // prepare data
            val duration = it.callDuration?.toIntOrNull() ?: -1
            val number = it.phoneNumber ?: ""
            val callStarted = it.callStartedTimestamp
            val callDirection = it.direction

            // prepare credentials
            val credentials = PreferencesUtils.loadCredentials(context)
            val projectId = PreferencesUtils.loadProjectId(context)
            val projectName = PreferencesUtils.loadProjectName(context)

            // store data
            val call = Call(
                callStarted,
                credentials?.user.toString(),
                credentials?.domain ?: "",
                projectId ?: "-1",
                projectName ?: "<none>",
                duration,
                callDirection,
                number,
                callStarted,
                0,
                0
            )

            uiCalls.add(call)
            val entity = CallEntity(
                call.callStarted,
                call.userId,
                call.domainId,
                call.projectId,
                "",
                call.projectName,
                null,
                call.direction.name,
                call.phoneNumber,
                call.callStarted,
                call.callEnded,
                call.callAccepted,
                call.duration,
            )

            syncCalls.add(entity)
        }
        if (syncCalls.isEmpty()) {
            PreferencesUtils.saveLastSyncDate(context, System.currentTimeMillis())
            state?.invoke("Synchornizace není potřeba, záznamy jsou aktuální.")
            return@launch
        }
        //upload calls
        uploadCall(context, syncCalls) { success ->
            if (!success) {
                state?.invoke("Synchronizace se nezdařila, zkontrolujte připojení k internetu.")
                //nothing to do
            } else {
                //toast sync success and insert to db
                syncCalls.forEach {
                    App.appDatabase.taskCalls().insert(it)
                }
                uiCalls.forEach { App.cacheStorage.addCallItem(it) }

                state?.invoke("Synchronizace proběhla úspěšně, nahráno a uloženo ${syncCalls.size} záznamů.")
                PreferencesUtils.saveLastSyncDate(context, System.currentTimeMillis())
            }
        }
    }

}