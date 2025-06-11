package cz.dzubera.callwarden.utils

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.model.CallHistory
import cz.dzubera.callwarden.service.db.CallEntity
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun startSynchronization(context: Context, state: ((String) -> Unit)?) {
    val calls = CallHistory.getCallsHistory(
        context,
        PreferencesUtils.loadSyncCount(context)
    ).toMutableList()
    CoroutineScope(Dispatchers.IO).launch {
        val callsFromDB = App.appDatabase.taskCalls().getAll()

        val syncCalls = mutableListOf<CallEntity>()
        val uiCalls = mutableListOf<Call>()
        calls.forEach {


            if (callsFromDB.any { entityDb -> (entityDb.uid == it.callStartedTimestamp) }) {
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
            var projectId = PreferencesUtils.loadProjectId(context)
            var projectName = PreferencesUtils.loadProjectName(context)

            val proj = App.projectStorage.getProject()
            if (proj != null) {
                projectId = proj.id
                projectName = proj.name
            }

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
                    try {
                        val results = App.appDatabase.taskCalls()
                        if (results.get(it.uid) == null) {
                            App.appDatabase.taskCalls().insert(it)
                        }

                    } catch (e: SQLiteConstraintException) {
                        Sentry.addBreadcrumb("Call already stored, trying to insert again")
                    }

                }
                uiCalls.forEach { App.cacheStorage.addCallItem(it) }

                state?.invoke("Synchronizace proběhla úspěšně, nahráno a uloženo ${syncCalls.size} záznamů.")
                PreferencesUtils.saveLastSyncDate(context, System.currentTimeMillis())
            }
        }
    }

}
