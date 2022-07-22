package cz.dzubera.callwarden

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DataSource(resources: Resources) {

    private val callsLiveData = MutableLiveData<List<Call>>(listOf())

    /* Adds flower to liveData and posts value. */
    fun addCall(call: Call) {
        val currentList = callsLiveData.value
        if (currentList == null) {
            callsLiveData.postValue(listOf(call))
        } else {
            val updatedList = currentList.toMutableList()
            updatedList.add(0, call)
            callsLiveData.postValue(updatedList)
        }
    }

    fun setCallList(calls: List<Call>) {
        callsLiveData.postValue(calls)
    }

    /* Removes flower from liveData and posts value. */
    fun removeCall(call: Call) {
        val currentList = callsLiveData.value
        if (currentList != null) {
            val updatedList = currentList.toMutableList()
            updatedList.remove(call)
            callsLiveData.postValue(updatedList)
        }
    }

    /* Returns flower given an ID. */
    fun getCall(id: Long): Call? {
        callsLiveData.value.let { calls ->
            return calls?.firstOrNull { it.id == id }
        }
    }

    fun getCallList(): LiveData<List<Call>> {
        return callsLiveData
    }


    companion object {
        private var INSTANCE: DataSource? = null

        fun getDataSource(resources: Resources): DataSource {
            return synchronized(DataSource::class) {
                val newInstance = INSTANCE ?: DataSource(resources)
                INSTANCE = newInstance
                newInstance
            }
        }
    }
}