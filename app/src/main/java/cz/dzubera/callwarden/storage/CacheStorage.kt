package cz.dzubera.callwarden.storage

import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.model.Call
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class CacheStorage {

    private val observers = mutableListOf<(list: List<Call>) -> Unit>()

    private val callItems = mutableListOf<Call>()

    private lateinit var lock: String
    fun initialize() {
        lock = String()
    }


    fun addCallItem(call: Call) {
        synchronized(lock) {
            if (!callItems.any {
                    it.id == call.id
                }) {
                callItems.add(call)
            }
            notifyObservers(callItems)
        }
    }

    fun getFromDB(result: (List<Call>) -> Unit) {
        val calls = mutableListOf<Call>()
        GlobalScope.launch {

            App.appDatabase.taskCalls().getAll().forEach {
                calls.add(
                    Call(
                        it.uid,
                        it.userId!!,
                        it.domainId!!,
                        it.projectId ?: "-1",
                        it.projectName ?: "<none>",
                        it.callDuration ?: -1,
                        Call.Direction.valueOf(it.direction!!),
                        it.phoneNumber!!,
                        it.callStarted!!,
                        it.callEnded!!,
                        it.callAccepted
                    )
                )
            }
            result.invoke(calls)
        }
    }

    fun loadFromDatabase(result: ((List<Call>) -> Unit)? = null) {
        val income = App.callTypeFilter[0]
        val outcome = App.callTypeFilter[1]
        val accepted = App.callTypeFilter[2]
        val unaccepted = App.callTypeFilter[3]

        getFromDB { calls ->
            synchronized(lock) {
                var newList = calls.filter { call: Call ->
                    App.dateFrom.before(Date(call.callStarted))
                            && App.dateTo.after(Date(call.callStarted))
                            && (App.projectFilter?.id == call.projectId
                            || (App.projectFilter == null
                            && !call.projectId.isEmpty()))
                }

                if(income && outcome){

                } else {
                    if (income) {
                        newList = newList.filter { it.direction == Call.Direction.INCOMING }
                    }
                    if (outcome) {
                        newList = newList.filter { it.direction == Call.Direction.OUTGOING }
                    }
                }

                if(accepted && unaccepted){

                } else {
                    if (accepted) {
                        newList = newList.filter { it.duration > 0 }
                    }
                    if (unaccepted) {
                        newList = newList.filter { it.duration <= 0 }
                    }
                }
                newList = newList.sortedByDescending { it.callStarted }
                callItems.clear()
                callItems.addAll(newList)
                result?.invoke(newList)
                notifyObservers(newList.toList())
            }
        }

    }

    fun getCallItems(): MutableList<Call> {
        return callItems
    }


    fun registerObserver(func: (list: List<Call>) -> Unit) {
        observers.add(func)
    }

    fun unregisterObserver(func: (list: List<Call>) -> Unit) {
        observers.add(func)
    }


    fun editCallItem(call: Call) {
        synchronized(lock) {
            callItems.removeIf { call.id == it.id }
            if (!callItems.any {
                    it.id == call.id
                }) {
                callItems.add(call)
            }
            notifyObservers(callItems)
        }


    }

    private fun notifyObservers(callItems: List<Call>) {
        synchronized(lock) {
            val newList = callItems.filter { call: Call ->
                App.dateFrom.before(Date(call.callStarted)) && App.dateTo.after(Date(call.callStarted)) && (App.projectFilter?.id == call.projectId || (App.projectFilter == null && !call.projectId.isEmpty()))
            }.sortedByDescending { it.callStarted }
            observers.forEach { it.invoke(newList) }
        }

    }

}
