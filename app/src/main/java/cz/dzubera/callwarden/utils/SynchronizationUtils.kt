package cz.dzubera.callwarden.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.core.content.ContextCompat
import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.model.CallHistory
import cz.dzubera.callwarden.service.db.CallEntity
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val syncMutex = Mutex()

fun startSynchronization(context: Context, state: ((String) -> Unit)?) {

    CoroutineScope(Dispatchers.IO).launch {

        syncMutex.withLock {

            val project = App.projectStorage.getProject(context)
            if (project == null) {
                state?.invoke("Vyberte projekt! Synchronizaci nelze provést bez vybraného projektu.")
                return@withLock
            }

            // Check permissions
            if (!hasCallLogPermissions(context)) {
                Log.e(
                    "SynchronizationUtils",
                    "READ_CALL_LOG or WRITE_CALL_LOG permission not granted"
                )
                state?.invoke("Synchronizace se nezdařila, chybí oprávnění pro čtení nebo zápis historie hovorů.")
                return@withLock
            }

            val credentials = PreferencesUtils.loadCredentials(context)

            // Get local call history
            val calls = CallHistory.getCallsHistory(
                context,
                PreferencesUtils.loadSyncCount(context)
            )

            val callsFromDB = App.appDatabase.taskCalls().getAll()
            val existingIds = callsFromDB.map { it.uid }.toSet()

            val callsToSync = calls
                .filter { it.callStartedTimestamp !in existingIds }
                .distinctBy { it.callStartedTimestamp }  // prevent duplicates coming from CallHistory
                .map { call ->
                    Log.d("Need to sync", call.callStartedTimestamp.toString())

                    val duration = call.callDuration?.toIntOrNull() ?: -1
                    val number = call.phoneNumber ?: ""

                    // Transform into app Call model
                    val uiCall = Call(
                        callStarted = call.callStartedTimestamp,
                        userId = credentials?.user.toString() ?: "",
                        domainId = credentials?.domain ?: "",
                        projectId = project.id ?: "-1",
                        projectName = project.name ?: "<none>",
                        duration = duration,
                        direction = call.direction,
                        phoneNumber = number,
                        callEnded = 0,
                        callAccepted = 0,
                        id = call.callStartedTimestamp,
                    )

                    val dbEntity = CallEntity(
                        uid = uiCall.callStarted,
                        userId = uiCall.userId,
                        domainId = uiCall.domainId,
                        projectId = uiCall.projectId,
                        projectName = uiCall.projectName,
                        direction = uiCall.direction.name,
                        phoneNumber = uiCall.phoneNumber,
                        callStarted = uiCall.callStarted,
                        callEnded = uiCall.callEnded,
                        callAccepted = uiCall.callAccepted,
                        callDuration = uiCall.duration,
                        projectIdOld = "",
                        type = null,
                    )

                    uiCall to dbEntity
                }

            if (callsToSync.isEmpty()) {
                PreferencesUtils.saveLastSyncDate(context, System.currentTimeMillis())
                state?.invoke("Synchronizace není potřeba, záznamy jsou aktuální.")
                return@withLock
            }

            // Upload calls to backend
            val dbEntities = callsToSync.map { it.second }

            uploadCall(context, dbEntities) { success ->
                if (!success) {
                    state?.invoke("Synchronizace se nezdařila, zkontrolujte připojení k internetu.")
                    return@uploadCall
                }

                // Insert only missing items
                dbEntities.forEach { entity ->
                    try {
                        if (App.appDatabase.taskCalls().get(entity.uid) == null) {
                            App.appDatabase.taskCalls().insert(entity)
                        }
                    } catch (e: SQLiteConstraintException) {
                        Sentry.addBreadcrumb("Call already stored, ignoring duplicate insert")
                    }
                }

                callsToSync.forEach { App.cacheStorage.addCallItem(it.first) }

                state?.invoke("Synchronizace proběhla úspěšně, nahráno a uloženo ${dbEntities.size} záznamů.")
                PreferencesUtils.saveLastSyncDate(context, System.currentTimeMillis())
            }
        }
    }
}

private fun hasCallLogPermissions(context: Context): Boolean {
    val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
    val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG)
    return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
}