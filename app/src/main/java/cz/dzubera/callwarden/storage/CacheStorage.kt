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
        synchronized(lock){
            if (!callItems.any {
                    it.id == call.id
                }) {
                callItems.add(call)
            }
            notifyObservers(callItems)
        }
    }

    fun getFromDB(result: (List<Call>) -> Unit){
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
                        Call.Type.valueOf(it.type!!),
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

    fun loadFromDatabase() {
        synchronized(lock){
            getFromDB{ calls ->
                val newList = calls.filter { call: Call ->
                    App.dateFrom.before(Date(call.callStarted)) && App.dateTo.after(Date(call.callStarted))
                }.sortedByDescending { it.callStarted }
                callItems.clear()
                callItems.addAll(newList)
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
       synchronized(lock){
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
        synchronized(lock){
            val newList = callItems.filter { call: Call ->
                App.dateFrom.before(Date(call.callStarted)) && App.dateTo.after(Date(call.callStarted))
            }.sortedByDescending { it.callStarted }
            observers.forEach { it.invoke(newList) }
        }

    }

}
