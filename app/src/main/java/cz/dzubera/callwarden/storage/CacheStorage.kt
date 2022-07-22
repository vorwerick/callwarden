package cz.dzubera.callwarden.storage

import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.Call
import java.util.*

class CacheStorage {

    private val observers = mutableListOf<(list: List<Call>) -> Unit>()

    private val callItems = mutableListOf<Call>()

    private val callItemsTemp = mutableListOf<Call>()

    fun addCallItem(call: Call) {
        callItems.add(call)
        callItemsTemp.add(call)
        observers.forEach { it.invoke(callItems) }
    }

    fun removeCallItem(call: Call) {
        callItems.remove(call)
        observers.forEach { it.invoke(callItems) }
    }

    fun loadFromDatabase() {
        callItems.clear()
        callItemsTemp.clear()

        val calls = mutableListOf<Call>()
        App.appDatabase.taskCalls()?.getAll()?.forEach {
            calls.add(
                Call(
                    it.uid,
                    Call.Type.valueOf(it.type!!),
                    Call.Direction.valueOf(it.direction!!),
                    it.phoneNumber!!,
                    it.callStarted!!,
                    it.callEnded!!,
                    it.callAccepted
                )
            )
        }
        callItemsTemp.addAll(calls)
        callItems.addAll(calls)


    }

    fun getCallItems(): MutableList<Call> {
        return callItems
    }

    fun clearCallItem(call: Call) {
        callItems.clear()
        observers.forEach { it.invoke(callItems) }
    }

    fun registerObserver(func: (list: List<Call>) -> Unit) {
        observers.add(func)
    }

    fun unregisterObserver(func: (list: List<Call>) -> Unit) {
        observers.add(func)
    }

    fun notifyItems() {
        observers.forEach { it.invoke(callItems) }
    }

    fun filter() {
        val newList = callItemsTemp.filter { call: Call ->
            Date(call.callStarted).before(App.dateTo) && Date(call.callStarted).after(App.dateFrom)
        }
        callItems.clear()
        callItems.addAll(newList)

        notifyItems()
    }
}
